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
package org.fabric3.api.host.domain;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;

import org.fabric3.api.model.type.component.Composite;

/**
 * Represents a domain.
 */
public interface Domain {

    /**
     * Include a deployable composite in the domain.
     *
     * @param deployable the name of the deployable composite to include
     * @throws DeploymentException if an error is encountered during inclusion
     */
    void include(QName deployable) throws DeploymentException;

    /**
     * Include a deployable composite in the domain using the specified DeploymentPlan.
     *
     * @param deployable the name of the deployable composite to include
     * @param plan       the deployment plan name
     * @throws DeploymentException if an error is encountered during inclusion
     */
    void include(QName deployable, String plan) throws DeploymentException;

    /**
     * Include all deployables contained in the list of contributions in the domain. If deployment plans are present in the composites, they will be used. This
     * operation is intended for composites that are synthesized from multiple deployable composites that are associated with individual deployment plans.
     *
     * @param uris the contributions to deploy
     * @throws DeploymentException if an error is encountered during inclusion
     */
    void include(List<URI> uris) throws DeploymentException;

    /**
     * Include a composite in the domain.
     *
     * @param composite the composite to include
     * @param simulated true if the include is a simulation. Simulated includes skip generation and deployment to runtimes. In addition, simulated deployments
     *                  are not fail-fast, i.e. they will be completed if assembly errors exist.
     * @throws DeploymentException if an error is encountered during inclusion
     */
    void include(Composite composite, boolean simulated) throws DeploymentException;

    /**
     * Remove all deployables in a contribution from the domain.
     *
     * @param uri   the contribution URI
     * @param force true if the undeployment operation should ignore errors from runtimes and remove logical components on the controller. If true, undeployment
     *              will also succeed if no participants are available.
     * @throws DeploymentException if an error is encountered during undeployment
     */
    void undeploy(URI uri, boolean force) throws DeploymentException;

    /**
     * Undeploys the composite.
     *
     * @param composite the composite
     * @param simulated true if the include is a simulation. Simulated includes skip generation and deployment to runtimes.
     * @throws DeploymentException if an error is encountered during undeployment
     */
    void undeploy(Composite composite, boolean simulated) throws DeploymentException;

    /**
     * Activates a set of definitions contained in the contribution.
     *
     * @param uri the contribution URI
     * @throws DeploymentException if an error is encountered during activation
     */
    void activateDefinitions(URI uri) throws DeploymentException;

    /**
     * Deactivates a set of definitions contained in the contribution.
     *
     * @param uri the contribution URI
     * @throws DeploymentException if an error is encountered during activation
     */
    void deactivateDefinitions(URI uri) throws DeploymentException;

    /**
     * Initiates a recovery operation using a journal containing the recorded domain state.
     *
     * @param journal the domain journal containing the recorded domain state
     * @throws DeploymentException if an error is encountered during recovery
     */
    void recover(DomainJournal journal) throws DeploymentException;

}
