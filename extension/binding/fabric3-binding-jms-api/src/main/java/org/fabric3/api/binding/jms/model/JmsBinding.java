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
package org.fabric3.api.binding.jms.model;

import java.net.URI;

import org.fabric3.api.model.type.component.Binding;

/**
 * Encapsulates JMS binding configuration specified in a composite.
 */
public class JmsBinding extends Binding {
    private URI generatedTargetUri;
    private JmsBindingMetadata metadata;

    /**
     * Constructor.
     *
     * @param metadata the JMS metadata
     */
    public JmsBinding(JmsBindingMetadata metadata) {
        this(null, null, metadata);
    }

    /**
     * Constructor.
     *
     * @param bindingName the binding name
     * @param metadata    the JMS metadata
     */
    public JmsBinding(String bindingName, JmsBindingMetadata metadata) {
        this(bindingName, null, metadata);
    }

    /**
     * Constructor.
     *
     * @param bindingName the binding name
     * @param targetURI   the binding target URI
     * @param metadata    the JMS metadata to be initialized
     */
    public JmsBinding(String bindingName, URI targetURI, JmsBindingMetadata metadata) {
        super(bindingName, targetURI, "jms");
        this.metadata = metadata;
    }

    public JmsBindingMetadata getJmsMetadata() {
        return metadata;
    }

    public void setJmsMetadata(JmsBindingMetadata metadata) {
        this.metadata = metadata;
    }

    public void setGeneratedTargetUri(URI generatedTargetUri) {
        this.generatedTargetUri = generatedTargetUri;
    }

    @Override
    public URI getTargetUri() {
        return generatedTargetUri;
    }

}
