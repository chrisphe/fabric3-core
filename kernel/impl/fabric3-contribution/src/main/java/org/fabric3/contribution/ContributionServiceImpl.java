/*
 * Fabric3
 * Copyright (c) 2009-2015 Metaform Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 */
package org.fabric3.contribution;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.fabric3.api.annotation.monitor.Monitor;
import org.fabric3.api.host.ContainerException;
import org.fabric3.api.host.contribution.ArtifactValidationFailure;
import org.fabric3.api.host.contribution.ContributionAlreadyInstalledException;
import org.fabric3.api.host.contribution.ContributionLockedException;
import org.fabric3.api.host.contribution.ContributionNotFoundException;
import org.fabric3.api.host.contribution.ContributionOrder;
import org.fabric3.api.host.contribution.ContributionService;
import org.fabric3.api.host.contribution.ContributionSource;
import org.fabric3.api.host.contribution.Deployable;
import org.fabric3.api.host.contribution.DuplicateContributionException;
import org.fabric3.api.host.contribution.DuplicateProfileException;
import org.fabric3.api.host.contribution.InstallException;
import org.fabric3.api.host.contribution.RemoveException;
import org.fabric3.api.host.contribution.StoreException;
import org.fabric3.api.host.contribution.UninstallException;
import org.fabric3.api.host.failure.ValidationFailure;
import org.fabric3.api.host.repository.Repository;
import org.fabric3.api.host.stream.Source;
import org.fabric3.api.host.stream.UrlSource;
import org.fabric3.api.model.type.component.Composite;
import org.fabric3.spi.contribution.Capability;
import org.fabric3.spi.contribution.ContentTypeResolutionException;
import org.fabric3.spi.contribution.ContentTypeResolver;
import org.fabric3.spi.contribution.Contribution;
import org.fabric3.spi.contribution.ContributionManifest;
import org.fabric3.spi.contribution.ContributionServiceListener;
import org.fabric3.spi.contribution.ContributionState;
import org.fabric3.spi.contribution.DependencyException;
import org.fabric3.spi.contribution.DependencyResolver;
import org.fabric3.spi.contribution.MetaDataStore;
import org.fabric3.spi.contribution.ProcessorRegistry;
import org.fabric3.spi.contribution.Resource;
import org.fabric3.spi.contribution.ResourceElement;
import org.fabric3.spi.contribution.manifest.QNameSymbol;
import org.fabric3.spi.introspection.DefaultIntrospectionContext;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.validation.InvalidContributionException;
import org.fabric3.spi.introspection.validation.ValidationUtils;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Reference;

/**
 * Default ContributionService implementation.
 */
@EagerInit
public class ContributionServiceImpl implements ContributionService {
    private ProcessorRegistry processorRegistry;
    private Repository repository;
    private MetaDataStore metaDataStore;
    private ContributionLoader contributionLoader;
    private ContentTypeResolver contentTypeResolver;
    private DependencyResolver dependencyResolver;
    private ContributionServiceMonitor monitor;
    private List<ContributionServiceListener> listeners;

    public ContributionServiceImpl(@Reference ProcessorRegistry processorRegistry,
                                   @Reference MetaDataStore metaDataStore,
                                   @Reference ContributionLoader contributionLoader,
                                   @Reference ContentTypeResolver contentTypeResolver,
                                   @Reference DependencyResolver dependencyResolver,
                                   @Monitor ContributionServiceMonitor monitor) {
        this.processorRegistry = processorRegistry;
        this.metaDataStore = metaDataStore;
        this.contributionLoader = contributionLoader;
        this.contentTypeResolver = contentTypeResolver;
        this.dependencyResolver = dependencyResolver;
        this.monitor = monitor;
        listeners = new ArrayList<>();
    }

    @Reference(required = false)
    public void setListeners(List<ContributionServiceListener> listeners) {
        this.listeners = listeners;
    }

    /**
     * Lazily injects the repository. Some environments may inject the repository via an extension loaded after bootstrap.
     *
     * @param repository the store to inject
     */
    @Reference(required = false)
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Set<URI> getContributions() {
        Set<Contribution> contributions = metaDataStore.getContributions();
        Set<URI> uris = new HashSet<>(contributions.size());
        for (Contribution contribution : contributions) {
            uris.add(contribution.getUri());
        }
        return uris;
    }

    public boolean exists(URI uri) {
        return metaDataStore.find(uri) != null;
    }

    public long getContributionTimestamp(URI uri) {
        Contribution contribution = metaDataStore.find(uri);
        if (contribution == null) {
            return -1;
        }
        return contribution.getTimestamp();
    }

    public List<Deployable> getDeployables(URI uri) throws ContributionNotFoundException {
        Contribution contribution = find(uri);
        List<Deployable> list = new ArrayList<>();
        if (contribution.getManifest() != null) {
            for (Deployable deployable : contribution.getManifest().getDeployables()) {
                list.add(deployable);
            }
        }
        return list;
    }

    public List<QName> getDeployedComposites(URI uri) throws ContributionNotFoundException {
        Contribution contribution = find(uri);
        List<QName> owners = contribution.getLockOwners();
        return new ArrayList<>(owners);
    }

    public URI store(ContributionSource contributionSource) throws StoreException {
        Contribution contribution = persist(contributionSource);
        metaDataStore.store(contribution);
        for (ContributionServiceListener listener : listeners) {
            listener.onStore(contribution);
        }
        return contribution.getUri();
    }

    public List<URI> store(List<ContributionSource> contributionSources) throws StoreException {
        List<URI> uris = new ArrayList<>();
        for (ContributionSource contributionSource : contributionSources) {
            URI uri = store(contributionSource);
            uris.add(uri);
        }
        return uris;
    }

    public void install(URI uri) throws InstallException, ContributionNotFoundException {
        install(Collections.singletonList(uri));
    }

    public List<URI> install(List<URI> uris) throws InstallException, ContributionNotFoundException {
        List<Contribution> contributions = new ArrayList<>(uris.size());
        for (URI uri : uris) {
            Contribution contribution = find(uri);
            contributions.add(contribution);
        }
        return installInOrder(contributions);
    }

    public void uninstall(URI uri) throws UninstallException, ContributionNotFoundException {
        Contribution contribution = find(uri);
        uninstall(contribution);
    }

    public void uninstall(List<URI> uris) throws UninstallException, ContributionNotFoundException {
        List<Contribution> contributions = new ArrayList<>(uris.size());
        for (URI uri : uris) {
            Contribution contribution = find(uri);
            contributions.add(contribution);
        }
        contributions = dependencyResolver.orderForUninstall(contributions);
        for (Contribution contribution : contributions) {
            uninstall(contribution);
        }
    }

    public void remove(URI uri) throws RemoveException, ContributionNotFoundException {
        Contribution contribution = find(uri);
        if (contribution.getState() != ContributionState.STORED) {
            throw new RemoveException("Contribution must first be uninstalled: " + uri);
        }
        metaDataStore.remove(uri);
        try {
            if (contribution.isPersistent()) {
                getRepository().remove(uri);
            }
        } catch (ContainerException e) {
            throw new RemoveException("Error removing contribution archive", e);
        }
        for (ContributionServiceListener listener : listeners) {
            listener.onRemove(contribution);
        }
    }

    public void remove(List<URI> uris) throws ContributionNotFoundException, RemoveException {
        for (URI uri : uris) {
            remove(uri);
        }
    }

    public boolean profileExists(URI uri) {
        for (Contribution contribution : metaDataStore.getContributions()) {
            if (contribution.getProfiles().contains(uri)) {
                return true;
            }
        }
        return false;
    }

    public List<URI> getContributionsInProfile(URI uri) {
        List<URI> profileContributions = new ArrayList<>();
        Set<Contribution> contributions = metaDataStore.getContributions();
        for (Contribution contribution : contributions) {
            if (contribution.getProfiles().contains(uri)) {
                profileContributions.add(contribution.getUri());
            }
        }
        return profileContributions;
    }

    public List<URI> getSortedContributionsInProfile(URI uri) {
        List<Contribution> sortedContributions = new ArrayList<>();
        Set<Contribution> contributions = metaDataStore.getContributions();
        for (Contribution contribution : contributions) {
            if (contribution.getProfiles().contains(uri)) {
                sortedContributions.add(contribution);
            }
        }
        List<URI> profileContributions = new ArrayList<>();
        sortedContributions = dependencyResolver.orderForUninstall(sortedContributions);
        for (Contribution contribution : sortedContributions) {
            profileContributions.add(contribution.getUri());
        }
        return profileContributions;
    }

    public void registerProfile(URI profileUri, List<URI> contributionUris) throws DuplicateProfileException {
        if (profileExists(profileUri)) {
            throw new DuplicateProfileException("Profile already installed: " + profileUri);
        }
        for (URI contributionUri : contributionUris) {
            Contribution contribution = metaDataStore.find(contributionUri);
            if (contribution == null) {
                throw new AssertionError("Contribution not found: " + contributionUri);
            }
            List<URI> profiles = contribution.getProfiles();
            if (!profiles.contains(profileUri)) {
                profiles.add(profileUri);
                for (ContributionServiceListener listener : listeners) {
                    listener.onUpdate(contribution);
                }
            }
        }
    }

    public void installProfile(URI uri) throws InstallException, ContributionNotFoundException {
        List<Contribution> toInstall = new ArrayList<>();
        for (Contribution contribution : metaDataStore.getContributions()) {
            if (contribution.getProfiles().contains(uri) && ContributionState.STORED == contribution.getState()) {
                toInstall.add(contribution);
            }
        }
        if (toInstall.isEmpty()) {
            throw new ContributionNotFoundException("Profile not found: " + uri);
        }
        installInOrder(toInstall);
    }

    public void uninstallProfile(URI uri) throws UninstallException, ContributionNotFoundException {
        List<Contribution> toUninstall = new ArrayList<>();
        for (Contribution contribution : metaDataStore.getContributions()) {
            List<URI> profiles = contribution.getProfiles();
            if (profiles.contains(uri)) {
                if (profiles.size() == 1) {
                    ContributionState state = contribution.getState();
                    if (ContributionState.INSTALLED != state) {
                        throw new UninstallException("Contribution not in installed state: " + state);
                    }
                    toUninstall.add(contribution);
                }
            }
        }
        toUninstall = dependencyResolver.orderForUninstall(toUninstall);
        for (Contribution contribution : toUninstall) {
            uninstall(contribution.getUri());
        }
    }

    public void removeProfile(URI uri) throws RemoveException, ContributionNotFoundException {
        List<Contribution> toRemove = new ArrayList<>();
        for (Contribution contribution : metaDataStore.getContributions()) {
            List<URI> profiles = contribution.getProfiles();
            if (profiles.contains(uri)) {
                if (profiles.size() == 1) {
                    ContributionState state = contribution.getState();
                    if (ContributionState.STORED != state) {
                        throw new RemoveException("Contribution not in stored state: " + state);
                    }
                    toRemove.add(contribution);
                } else {
                    contribution.removeProfile(uri);
                }
            }
        }
        for (Contribution contribution : toRemove) {
            remove(contribution.getUri());
        }
    }

    public ContributionOrder processManifests(List<ContributionSource> contributionSources) throws InstallException, StoreException {
        List<Contribution> contributions = new ArrayList<>();
        for (ContributionSource contributionSource : contributionSources) {
            // store the contributions
            Contribution contribution = persist(contributionSource);
            metaDataStore.store(contribution);
            for (ContributionServiceListener listener : listeners) {
                listener.onStore(contribution);
            }
            contributions.add(contribution);
        }
        return introspectManifests(contributions);
    }

    public void processContents(URI uri) throws InstallException, ContributionNotFoundException {
        Contribution contribution = find(uri);
        try {
            ClassLoader loader = contributionLoader.load(contribution);
            // continue processing the contributions. As they are ordered, dependencies will resolve correctly
            processContents(contribution, loader);
            contribution.setState(ContributionState.INSTALLED);
            for (ContributionServiceListener listener : listeners) {
                listener.onInstall(contribution);
            }
        } catch (InstallException e) {
            try {
                revertInstall(Collections.singletonList(contribution));
            } catch (RuntimeException ex) {
                monitor.error("Error reverting deployment", ex);
            }
            throw e;
        }
        String description = contribution.getManifest().getDescription();
        if (description != null) {
            monitor.installed(description);
        }
    }

    private ContributionOrder introspectManifests(List<Contribution> contributions) throws InstallException {
        ContributionOrder order = new ContributionOrder();
        for (Contribution contribution : contributions) {
            if (ContributionState.STORED != contribution.getState()) {
                throw new ContributionAlreadyInstalledException("Contribution is already installed: " + contribution.getUri());
            }
        }
        for (Contribution contribution : contributions) {
            // process any SCA manifest information, including imports and exports
            processManifest(contribution);
        }
        // order the contributions based on their dependencies
        try {
            contributions = dependencyResolver.resolve(contributions);
        } catch (DependencyException e) {
            throw new InstallException(e);
        }
        for (Contribution contribution : contributions) {
            boolean requiresLoad = false;
            ContributionManifest manifest = contribution.getManifest();
            for (Capability capability : manifest.getRequiredCapabilities()) {
                if (capability.requiresLoad()) {
                    requiresLoad = true;
                    break;
                }
            }
            if (requiresLoad) {
                order.addIsolatedContribution(contribution.getUri());
            } else {
                order.addBaseContribution(contribution.getUri());
            }
        }
        return order;
    }

    /**
     * Resolves a contribution by its URI.
     *
     * @param uri the contribution URI
     * @return the contribution
     * @throws ContributionNotFoundException if the contribution does not exist
     */
    private Contribution find(URI uri) throws ContributionNotFoundException {
        Contribution contribution = metaDataStore.find(uri);
        if (contribution == null) {
            throw new ContributionNotFoundException("Contribution not found: " + uri);
        }
        return contribution;
    }

    /**
     * Installs a collection of contributions in order of their dependencies.
     *
     * @param contributions the contributions
     * @return the ordered list of contribution URIs
     * @throws InstallException if there is an error installing the contributions
     */
    private List<URI> installInOrder(List<Contribution> contributions) throws InstallException {
        for (Contribution contribution : contributions) {
            if (ContributionState.STORED != contribution.getState()) {
                throw new ContributionAlreadyInstalledException("Contribution is already installed: " + contribution.getUri());
            }
        }
        for (Contribution contribution : contributions) {
            // process any SCA manifest information, including imports and exports
            processManifest(contribution);
        }
        // order the contributions based on their dependencies
        try {
            contributions = dependencyResolver.resolve(contributions);
        } catch (DependencyException e) {
            throw new InstallException(e);
        }
        try {
            for (Contribution contribution : contributions) {
                ClassLoader loader = contributionLoader.load(contribution);
                // continue processing the contributions. As they are ordered, dependencies will resolve correctly
                processContents(contribution, loader);
                contribution.setState(ContributionState.INSTALLED);
                for (ContributionServiceListener listener : listeners) {
                    listener.onInstall(contribution);
                }
            }
        } catch (InstallException e) {
            try {
                revertInstall(contributions);
            } catch (RuntimeException ex) {
                monitor.error("Error reverting deployment", ex);
            }
            throw e;
        }
        List<URI> uris = new ArrayList<>(contributions.size());
        for (Contribution contribution : contributions) {
            URI uri = contribution.getUri();
            uris.add(uri);
            String description = contribution.getManifest().getDescription();
            if (description != null) {
                monitor.installed(description);
            } else if (!contribution.getManifest().isExtension()) {
                monitor.installed(uri.toString());
            }
        }
        return uris;
    }

    private void revertInstall(List<Contribution> contributions) {
        ListIterator<Contribution> iterator = contributions.listIterator(contributions.size());
        while (iterator.hasPrevious()) {
            Contribution contribution = iterator.previous();
            try {
                if (ContributionState.INSTALLED == contribution.getState()) {
                    uninstall(contribution);
                }
                contributionLoader.unload(contribution);
                remove(contribution.getUri());
            } catch (UninstallException | RemoveException | ContributionNotFoundException ex) {
                monitor.error("Error reverting installation: " + contribution.getUri(), ex);
            }
        }
    }

    private void uninstall(Contribution contribution) throws UninstallException {
        URI uri = contribution.getUri();
        if (contribution.getState() != ContributionState.INSTALLED) {
            throw new UninstallException("Contribution not installed: " + uri);
        }
        if (contribution.isLocked()) {
            List<QName> deployables = contribution.getLockOwners();
            throw new ContributionLockedException("Contribution is currently in use by a deployment: " + uri, uri, deployables);
        }
        // unload from memory
        contributionLoader.unload(contribution);
        contribution.setState(ContributionState.STORED);
        for (ContributionServiceListener listener : listeners) {
            listener.onUninstall(contribution);
        }
        String description = contribution.getManifest().getDescription();
        if (description != null) {
            monitor.uninstalled(description);
        } else if (!contribution.getManifest().isExtension()) {
            monitor.uninstalled(uri.toString());
        }
    }

    /**
     * Processes the contribution manifest.
     *
     * @param contribution the contribution
     * @throws InstallException if there is an error during introspection such as an invalid contribution
     */
    private void processManifest(Contribution contribution) throws InstallException {
        IntrospectionContext context = new DefaultIntrospectionContext();
        processorRegistry.processManifest(contribution, context);
        if (context.hasErrors()) {
            URI uri = contribution.getUri();
            ArtifactValidationFailure failure = new ArtifactValidationFailure(uri, "the contribution manifest (sca-contribution.xml)");
            failure.addFailures(context.getErrors());
            List<ValidationFailure> failures = new ArrayList<>();
            failures.add(failure);
            ArtifactValidationFailure warning = new ArtifactValidationFailure(uri, "the contribution manifest (sca-contribution.xml)");
            warning.addFailures(context.getWarnings());
            List<ValidationFailure> warnings = new ArrayList<>();
            warnings.add(warning);
            throw new InvalidContributionException(failures, warnings);
        }
        for (ContributionServiceListener listener : listeners) {
            listener.onProcessManifest(contribution);
        }

    }

    /**
     * Processes contribution contents. This assumes all dependencies are installed and can be resolved.
     *
     * @param contribution the contribution to process
     * @param loader       the classloader to load resources in
     * @throws InstallException if an error occurs during processing
     */
    private void processContents(Contribution contribution, ClassLoader loader) throws InstallException {
        try {
            URI contributionUri = contribution.getUri();
            IntrospectionContext context = new DefaultIntrospectionContext(contributionUri, loader);
            processorRegistry.indexContribution(contribution, context);
            if (context.hasErrors()) {
                throw new InvalidContributionException(context.getErrors(), context.getWarnings());
            } else if (context.hasWarnings()) {
                // there were just warnings, report them
                monitor.contributionWarnings(ValidationUtils.outputWarnings(context.getWarnings()));
            }
            metaDataStore.store(contribution);
            context = new DefaultIntrospectionContext(contributionUri, loader);
            processorRegistry.processContribution(contribution, context);
            validateContribution(contribution, context);
            if (context.hasErrors()) {
                throw new InvalidContributionException(context.getErrors(), context.getWarnings());
            } else if (context.hasWarnings()) {
                // there were just warnings, report them
                monitor.contributionWarnings(ValidationUtils.outputWarnings(context.getWarnings()));
            }
            addDeployableEntries(contribution);
        } catch (StoreException e) {
            throw new InstallException(e);
        }
    }

    /**
     * Performs final validation on a contribution.
     *
     * @param contribution the contribution to validate
     * @param context      the validation context
     */
    private void validateContribution(Contribution contribution, IntrospectionContext context) {
        for (Deployable deployable : contribution.getManifest().getDeployables()) {
            QName name = deployable.getName();
            QNameSymbol symbol = new QNameSymbol(name);
            boolean found = false;
            for (Resource resource : contribution.getResources()) {
                for (ResourceElement<?, ?> element : resource.getResourceElements()) {
                    if (element.getSymbol().equals(symbol)) {
                        found = true;
                    }
                }
            }
            if (!found) {
                URI uri = contribution.getUri();
                InvalidDeployable failure = new InvalidDeployable("Deployable composite " + name + " not found in " + uri, name);
                context.addError(failure);
            }

        }
    }

    /**
     * Adds deployable entries for composites that are defined as deployables but do not have an explicit entry in the manifest.
     *
     * @param contribution the contribution
     */
    private void addDeployableEntries(Contribution contribution) {
        ContributionManifest manifest = contribution.getManifest();
        for (Resource resource : contribution.getResources()) {
            for (ResourceElement<?, ?> element : resource.getResourceElements()) {
                if (element.getValue() instanceof Composite) {
                    Composite composite = (Composite) element.getValue();
                    if (composite.isDeployable()) {
                        Deployable deployable = new Deployable(composite.getName(), composite.getModes(), composite.getEnvironments());
                        if (!manifest.getDeployables().contains(deployable)) {
                            manifest.getDeployables().add(deployable);
                        }
                    }
                }
            }
        }
    }

    /**
     * Stores the contents of a contribution in the archive store if it is not local
     *
     * @param contributionSource the contribution source
     * @return the contribution
     * @throws StoreException if an error occurs during the store operation
     */
    private Contribution persist(ContributionSource contributionSource) throws StoreException {
        URI contributionUri = contributionSource.getUri();
        if (metaDataStore.find(contributionUri) != null) {
            throw new DuplicateContributionException("Contribution already exists: " + contributionUri);
        }
        URL locationUrl;
        boolean persistent = contributionSource.persist();
        Source source;
        if (!persistent) {
            locationUrl = contributionSource.getLocation();
            // reuse the source as the contribution is locally resolvable
            source = contributionSource.getSource();
        } else {
            InputStream stream = null;
            try {
                stream = contributionSource.getSource().openStream();
                boolean extension = contributionSource.isExtension();
                locationUrl = getRepository().store(contributionUri, stream, extension);
                source = new UrlSource(locationUrl);
            } catch (IOException | ContainerException e) {
                throw new StoreException(e);
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException e) {
                    monitor.error("Error closing contribution stream", e);
                }
            }
        }
        String type = contributionSource.getContentType();
        if (type == null && locationUrl == null) {
            throw new StoreException("Content type could not be determined for contribution: " + contributionUri);
        }
        if (type == null) {
            try {
                type = contentTypeResolver.getContentType(locationUrl);
            } catch (ContentTypeResolutionException e) {
                throw new StoreException(e);
            }
        }
        long timestamp = contributionSource.getTimestamp();
        return new Contribution(contributionUri, source, locationUrl, timestamp, type, persistent);
    }

    private Repository getRepository() {
        if (repository == null) {
            throw new UnsupportedOperationException(Repository.class.getSimpleName() + " not configured");
        }
        return repository;
    }
}
