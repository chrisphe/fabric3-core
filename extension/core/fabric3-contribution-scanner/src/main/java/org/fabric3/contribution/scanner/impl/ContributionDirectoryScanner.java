/*
* Fabric3
* Copyright (c) 2009-2013 Metaform Systems
*
* Fabric3 is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as
* published by the Free Software Foundation, either version 3 of
* the License, or (at your option) any later version, with the
* following exception:
*
* Linking this software statically or dynamically with other
* modules is making a combined work based on this software.
* Thus, the terms and conditions of the GNU General Public
* License cover the whole combination.
*
* As a special exception, the copyright holders of this software
* give you permission to link this software with independent
* modules to produce an executable, regardless of the license
* terms of these independent modules, and to copy and distribute
* the resulting executable under terms of your choice, provided
* that you also meet, for each linked independent module, the
* terms and conditions of the license of that module. An
* independent module is a module which is not derived from or
* based on this software. If you modify this software, you may
* extend this exception to your version of the software, but
* you are not obligated to do so. If you do not wish to do so,
* delete this exception statement from your version.
*
* Fabric3 is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty
* of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the
* GNU General Public License along with Fabric3.
* If not, see <http://www.gnu.org/licenses/>.
*/
package org.fabric3.contribution.scanner.impl;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.oasisopen.sca.annotation.Destroy;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Init;
import org.oasisopen.sca.annotation.Property;
import org.oasisopen.sca.annotation.Reference;

import org.fabric3.api.annotation.monitor.Monitor;
import org.fabric3.contribution.scanner.spi.FileSystemResource;
import org.fabric3.contribution.scanner.spi.FileSystemResourceFactoryRegistry;
import org.fabric3.contribution.scanner.spi.FileSystemResourceState;
import org.fabric3.api.host.contribution.ContributionException;
import org.fabric3.api.host.contribution.ContributionNotFoundException;
import org.fabric3.api.host.contribution.ContributionService;
import org.fabric3.api.host.contribution.ContributionSource;
import org.fabric3.api.host.contribution.FileContributionSource;
import org.fabric3.api.host.contribution.RemoveException;
import org.fabric3.api.host.contribution.UninstallException;
import org.fabric3.api.host.contribution.ValidationException;
import org.fabric3.api.host.domain.AssemblyException;
import org.fabric3.api.host.domain.DeploymentException;
import org.fabric3.api.host.domain.Domain;
import org.fabric3.api.host.runtime.HostInfo;
import org.fabric3.spi.runtime.event.EventService;
import org.fabric3.spi.runtime.event.ExtensionsInitialized;
import org.fabric3.spi.runtime.event.Fabric3Event;
import org.fabric3.spi.runtime.event.Fabric3EventListener;
import org.fabric3.spi.runtime.event.RuntimeStart;

/**
 * Scans deployment directories for contributions. In production mode, deployment directories will be scanned once at startup and any contained contributions
 * will be deployed. In the default dynamic (non-production) mode, scanning will be periodic with support for adding, updating, and removing contributions.
 * <p/>
 * In dynamic mode, the scanner watches deployment directories at a fixed interval. Files are tracked as a {@link FileSystemResource}, which provides a
 * consistent view across various types such as jars and exploded directories. Unknown file types are ignored. At the specified interval, removed files are
 * determined by comparing the current directory contents with the contents from the previous pass. Changes or additions are also determined by comparing the
 * current directory state with that of the previous pass. Detected changes and additions are cached for the following interval. Detected changes and additions
 * from the previous interval are then compared using a timestamp to see if they have changed again. If so, they remain cached. If they have not changed, they
 * are processed, contributed via the ContributionService, and deployed in the domain.
 */
@EagerInit
public class ContributionDirectoryScanner implements Runnable, Fabric3EventListener {
    private ContributionService contributionService;
    private FileSystemResourceFactoryRegistry registry;
    private EventService eventService;
    private ContributionTracker tracker;
    private ScannerMonitor monitor;
    private Domain domain;
    private List<File> paths;
    private long delay = 2000;
    private boolean production = false;

    private ScheduledExecutorService executor;
    private Set<File> ignored = new HashSet<File>();
    private Map<String, FileSystemResource> cache = new HashMap<String, FileSystemResource>();
    List<URI> notSeen = new ArrayList<URI>(); // contributions added when the runtime was offline and hence not previously seen by the scanner

    public ContributionDirectoryScanner(@Reference ContributionService contributionService,
                                        @Reference(name = "assembly") Domain domain,
                                        @Reference FileSystemResourceFactoryRegistry registry,
                                        @Reference EventService eventService,
                                        @Reference ContributionTracker tracker,
                                        @Reference HostInfo hostInfo,
                                        @Monitor ScannerMonitor monitor) {
        this.registry = registry;
        this.contributionService = contributionService;
        this.domain = domain;
        this.eventService = eventService;
        this.tracker = tracker;
        paths = hostInfo.getDeployDirectories();
        this.monitor = monitor;
    }

    @Property(required = false)
    public void setProduction(boolean production) {
        this.production = production;
    }

    @Property(required = false)
    public void setDelay(long delay) {
        this.delay = delay;
    }

    @SuppressWarnings({"unchecked"})
    @Init
    public void init() {
        eventService.subscribe(ExtensionsInitialized.class, this);
        // Register to be notified when the runtime starts to perform the following:
        //  1. Contributions installed to deployment directories when the runtime is offline are deployed to the domain
        //  2. The scanner thread is initialized
        eventService.subscribe(RuntimeStart.class, this);
    }

    @Destroy
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public void onEvent(Fabric3Event event) {
        if (event instanceof ExtensionsInitialized) {
            if (paths == null) {
                return;
            }
            // process existing files in recovery mode
            List<File> files = new ArrayList<File>();
            for (File path : paths) {
                File[] pathFiles = path.listFiles();
                if (pathFiles != null) {
                    Collections.addAll(files, pathFiles);
                }
            }
            processFiles(files, true);
        } else if (event instanceof RuntimeStart) {
            try {
                domain.include(notSeen);
            } catch (DeploymentException e) {
                monitor.error(e);
            }
            notSeen.clear();
            if (!production) {
                executor = Executors.newSingleThreadScheduledExecutor();
                executor.scheduleWithFixedDelay(this, 10, delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    public synchronized void run() {
        if (paths == null) {
            return;
        }
        List<File> files = new ArrayList<File>();
        for (File path : paths) {
            if (!path.isDirectory()) {
                // there is no extension directory, return without processing
                continue;
            }
            File[] pathFiles = path.listFiles();
            if (pathFiles != null) {
                Collections.addAll(files, pathFiles);
            }
        }
        processRemovals(files);
        if (files.isEmpty()) {
            // there are no files to process
            return;
        }
        try {
            processFiles(files, false);
            processIgnored();
        } catch (RuntimeException e) {
            monitor.error(e);
        } catch (Error e) {
            monitor.error(e);
            throw e;
        }
    }

    /**
     * Processes the contents of deployment directories and updates the state of cached resources based on those contents.
     *
     * @param files   the files in the deployment directories
     * @param recover true if processing is performed during recovery
     */
    private synchronized void processFiles(List<File> files, boolean recover) {
        for (File file : files) {
            String name = file.getName();
            FileSystemResource cached = cache.get(name);
            if (cached == null) {
                cached = registry.createResource(file);
                if (cached == null) {
                    // not a known type, ignore
                    if (!name.startsWith(".") && !name.endsWith(".txt") && !ignored.contains(file)) {
                        monitor.ignored(name);
                    }
                    ignored.add(file);
                    continue;
                }
                cache.put(name, cached);
                if (recover) {
                    // recover, do not wait to install
                    cached.setState(FileSystemResourceState.ADDED);
                } else {
                    // file may have been ignored previously as it was incomplete such as missing a manifest; remove it from the ignored list
                    ignored.remove(file);
                    continue;
                }
            } else {
                if (cached.getState() == FileSystemResourceState.ERROR) {
                    if (cached.isChanged()) {
                        // file has changed since the error was reported, set to detected
                        cached.setState(FileSystemResourceState.DETECTED);
                        cached.checkpoint();
                    } else {
                        // corrupt file from a previous run, continue
                        continue;
                    }
                } else if (cached.getState() == FileSystemResourceState.DETECTED) {
                    if (cached.isChanged()) {
                        // updates may still be pending, wait until the next pass
                        continue;
                    } else {
                        cached.setState(FileSystemResourceState.ADDED);
                        cached.checkpoint();
                    }
                } else if (cached.getState() == FileSystemResourceState.PROCESSED) {
                    if (cached.isChanged()) {
                        cached.setState(FileSystemResourceState.UPDATED);
                        cached.checkpoint();
                    }
                }
            }
        }
        if (recover) {
            processAdditions(true);
        } else {
            processUpdates();
            processAdditions(false);
        }
    }

    /**
     * Processes updated resources in the deployment directories.
     */
    private synchronized void processUpdates() {
        List<ContributionSource> sources = new ArrayList<ContributionSource>();
        List<FileSystemResource> updatedResources = new ArrayList<FileSystemResource>();
        List<URI> uris = new ArrayList<URI>();
        for (FileSystemResource resource : cache.values()) {
            if (resource.getState() != FileSystemResourceState.UPDATED) {
                continue;
            }
            try {
                String name = resource.getName();
                URI artifactUri = new URI(name);
                URL location = resource.getLocation();
                long timestamp = resource.getTimestamp();
                // undeploy any deployed composites in the reverse order that they were deployed in
                try {
                    domain.undeploy(artifactUri, false);
                } catch (DeploymentException e) {
                    monitor.error(e);
                    return;
                }
                // if the resource has changed, wait until the next pass as updates may still be in progress
                if (resource.isChanged()) {
                    resource.checkpoint();
                    continue;
                }
                ContributionSource source = new FileContributionSource(artifactUri, location, timestamp, false);
                sources.add(source);
                updatedResources.add(resource);
                uris.add(artifactUri);
            } catch (URISyntaxException e) {
                resource.setState(FileSystemResourceState.ERROR);
                monitor.error(e);
            }
        }
        try {
            if (!uris.isEmpty()) {
                contributionService.uninstall(uris);
                contributionService.remove(uris);
            }
            if (!sources.isEmpty()) {
                List<URI> stored = contributionService.store(sources);
                List<URI> contributions = contributionService.install(stored);
                domain.include(contributions);
                for (FileSystemResource resource : updatedResources) {
                    resource.setState(FileSystemResourceState.PROCESSED);
                    resource.checkpoint();
                    monitor.processed(resource.getName());
                }
            }
        } catch (ContributionException e) {
            for (FileSystemResource resource : updatedResources) {
                resource.setState(FileSystemResourceState.ERROR);
            }
            monitor.error(e);
        } catch (DeploymentException e) {
            for (FileSystemResource resource : updatedResources) {
                resource.setState(FileSystemResourceState.ERROR);
            }
            // back out installation
            revertInstallation(uris);
            monitor.error(e);
        }
    }

    private void revertInstallation(List<URI> uris) {
        try {
            contributionService.uninstall(uris);
            contributionService.remove(uris);
        } catch (UninstallException | RemoveException | ContributionNotFoundException e) {
            monitor.error(e);
        }
    }

    /**
     * Processes added resources in the deployment directories.
     *
     * @param recover true if files are being added in recovery mode
     */
    private synchronized void processAdditions(boolean recover) {
        List<ContributionSource> sources = new ArrayList<ContributionSource>();
        List<FileSystemResource> addedResources = new ArrayList<FileSystemResource>();

        for (FileSystemResource resource : cache.values()) {
            if (resource.getState() != FileSystemResourceState.ADDED || resource.isChanged()) {
                resource.checkpoint();
                continue;
            }
            String name = resource.getName();

            URL location = resource.getLocation();
            long timestamp = resource.getTimestamp();
            URI uri = URI.create(name);
            ContributionSource source = new FileContributionSource(uri, location, timestamp, false);
            sources.add(source);
            addedResources.add(resource);

            boolean seen = tracker.isTracked(name);
            if (!seen && recover) {
                // the contribution was not seen previously, schedule it to be deployed when the domain recovers
                notSeen.add(uri);
            }

            // track the addition
            tracker.addResource(name);

        }
        if (!sources.isEmpty()) {
            try {
                // Install contributions, which will be ordered transitively by import dependencies
                List<URI> stored = contributionService.store(sources);
                List<URI> addedUris = contributionService.install(stored);
                // Include the contributions if this is not a recovery operation (recovery will handle inclusion separately)
                if (!recover) {
                    domain.include(addedUris);
                }
                for (FileSystemResource resource : addedResources) {
                    resource.setState(FileSystemResourceState.PROCESSED);
                    resource.checkpoint();
                    monitor.processed(resource.getName());
                }
            } catch (ValidationException e) {
                // remove from not seen: FABRICTHREE-583
                for (ContributionSource source : sources) {
                    notSeen.remove(source.getUri());
                }
                // print out the validation errors
                monitor.contributionErrors(e.getMessage());
                for (FileSystemResource resource : addedResources) {
                    resource.setState(FileSystemResourceState.ERROR);
                }
            } catch (AssemblyException e) {
                // print out the deployment errors
                monitor.deploymentErrors(e.getMessage());
                for (FileSystemResource resource : addedResources) {
                    resource.setState(FileSystemResourceState.ERROR);
                }
            } catch (ContributionException | NoClassDefFoundError | DeploymentException e) {
                handleError(e, addedResources);
            } catch (Error | RuntimeException e) {
                for (FileSystemResource resource : addedResources) {
                    resource.setState(FileSystemResourceState.ERROR);
                }
                // re-throw the exception as the runtime may be in an unstable state
                throw e;
            }
        }
    }

    /**
     * process removed files, which results in undeployment.
     *
     * @param files the current contents of the deployment directories
     */
    private synchronized void processRemovals(List<File> files) {
        Map<String, File> index = new HashMap<String, File>(files.size());
        for (File file : files) {
            index.put(file.getName(), file);
        }

        for (Iterator<FileSystemResource> iterator = cache.values().iterator(); iterator.hasNext(); ) {
            FileSystemResource entry = iterator.next();
            String name = entry.getName();

            URI uri = URI.create(name);
            if (index.get(name) == null) {
                // artifact was removed
                try {
                    // track the removal
                    tracker.removeResource(name);
                    iterator.remove();
                    // check that the resource was not deleted by another process
                    if (contributionService.exists(uri)) {
                        domain.undeploy(uri, false);
                        contributionService.uninstall(uri);
                        contributionService.remove(uri);
                    }
                    monitor.removed(name);
                } catch (ContributionException | DeploymentException e) {
                    monitor.removalError(name, e);
                }
            }
        }
    }

    private synchronized void processIgnored() {
        for (Iterator<File> iter = ignored.iterator(); iter.hasNext(); ) {
            File file = iter.next();
            if (!file.exists()) {
                iter.remove();
            }
        }
    }

    private void handleError(Throwable e, List<FileSystemResource> addedResources) {
        monitor.error(e);
        for (FileSystemResource resource : addedResources) {
            resource.setState(FileSystemResourceState.ERROR);
        }
    }

}
