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
package org.fabric3.fabric.container.command;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.spi.container.builder.ComponentBuilder;
import org.fabric3.spi.container.builder.ComponentBuilderListener;
import org.fabric3.spi.container.component.Component;
import org.fabric3.spi.container.component.ComponentManager;
import org.fabric3.spi.model.physical.PhysicalComponent;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Init;
import org.oasisopen.sca.annotation.Reference;

/**
 * De-registers the component from the component manager.
 */
@EagerInit
public class DisposeComponentCommandExecutor implements CommandExecutor<DisposeComponentCommand> {
    private CommandExecutorRegistry executorRegistry;
    private ComponentManager componentManager;
    private Map<Class<?>, ComponentBuilder> builders;
    private List<ComponentBuilderListener> listeners = Collections.emptyList();

    public DisposeComponentCommandExecutor(@Reference CommandExecutorRegistry executorRegistry, @Reference ComponentManager componentManager) {
        this.executorRegistry = executorRegistry;
        this.componentManager = componentManager;
    }

    @Reference(required = false)
    public void setBuilders(Map<Class<?>, ComponentBuilder> builders) {
        this.builders = builders;
    }

    @Reference(required = false)
    public void setListeners(List<ComponentBuilderListener> listeners) {
        this.listeners = listeners;
    }

    @Init
    public void init() {
        executorRegistry.register(DisposeComponentCommand.class, this);
    }

    @SuppressWarnings({"unchecked"})
    public void execute(DisposeComponentCommand command) {
        PhysicalComponent physicalComponent = command.getComponent();
        URI uri = physicalComponent.getComponentUri();
        Component component = componentManager.unregister(uri);
        ComponentBuilder builder = builders.get(physicalComponent.getClass());
        if (builder == null) {
            throw new Fabric3Exception("Builder not found for " + physicalComponent.getClass().getName());
        }
        builder.dispose(physicalComponent, component);
        for (ComponentBuilderListener listener : listeners) {
            listener.onDispose(component, physicalComponent);
        }
    }

}