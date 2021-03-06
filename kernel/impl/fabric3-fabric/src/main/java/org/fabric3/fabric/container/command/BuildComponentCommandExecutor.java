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
import org.oasisopen.sca.annotation.Constructor;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Init;
import org.oasisopen.sca.annotation.Reference;

/**
 * Builds a component on a runtime.
 */
@EagerInit
public class BuildComponentCommandExecutor implements CommandExecutor<BuildComponentCommand> {

    private ComponentManager componentManager;
    private CommandExecutorRegistry commandExecutorRegistry;
    private Map<Class<?>, ComponentBuilder> builders;
    private List<ComponentBuilderListener> listeners = Collections.emptyList();

    @Constructor
    public BuildComponentCommandExecutor(@Reference ComponentManager componentManager, @Reference CommandExecutorRegistry commandExecutorRegistry) {
        this.componentManager = componentManager;
        this.commandExecutorRegistry = commandExecutorRegistry;
    }

    public BuildComponentCommandExecutor(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    @Init
    public void init() {
        commandExecutorRegistry.register(BuildComponentCommand.class, this);
    }

    @Reference(required = false)
    public void setBuilders(Map<Class<?>, ComponentBuilder> builders) {
        this.builders = builders;
    }

    @Reference(required = false)
    public void setListeners(List<ComponentBuilderListener> listeners) {
        this.listeners = listeners;
    }

    public void execute(BuildComponentCommand command) {
        PhysicalComponent physicalComponent = command.getComponent();
        Component component = build(physicalComponent);
        URI contributionUri = physicalComponent.getContributionUri();
        componentManager.register(component);
        for (ComponentBuilderListener listener : listeners) {
            listener.onBuild(component, physicalComponent);
        }
    }

    /**
     * Builds a physical component from a physical component.
     *
     * @param physicalComponent the physical component
     * @return Component to be built.
     */
    @SuppressWarnings("unchecked")
    private Component build(PhysicalComponent physicalComponent) {

        ComponentBuilder builder = builders.get(physicalComponent.getClass());
        if (builder == null) {
            throw new Fabric3Exception("Builder not found for " + physicalComponent.getClass().getName());
        }
        return builder.build(physicalComponent);
    }

}
