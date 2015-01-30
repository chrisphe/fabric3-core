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
package org.fabric3.fabric.domain.generator.resource;

import java.util.ArrayList;
import java.util.List;

import org.oasisopen.sca.annotation.Reference;

import org.fabric3.fabric.container.command.DisposeResourcesCommand;
import org.fabric3.fabric.domain.generator.CommandGenerator;
import org.fabric3.fabric.domain.generator.GeneratorRegistry;
import org.fabric3.api.model.type.component.Resource;
import org.fabric3.spi.domain.generator.GenerationException;
import org.fabric3.spi.domain.generator.resource.ResourceGenerator;
import org.fabric3.spi.model.instance.LogicalComponent;
import org.fabric3.spi.model.instance.LogicalCompositeComponent;
import org.fabric3.spi.model.instance.LogicalResource;
import org.fabric3.spi.model.instance.LogicalState;
import org.fabric3.spi.model.physical.PhysicalResourceDefinition;

/**
 * Creates a command to un-build a resource on a runtime.
 */
public class DisposeResourceCommandGenerator implements CommandGenerator {
    private GeneratorRegistry generatorRegistry;

    public DisposeResourceCommandGenerator(@Reference GeneratorRegistry generatorRegistry) {
        this.generatorRegistry = generatorRegistry;
    }

    public int getOrder() {
        return DISPOSE_RESOURCES;
    }

    @SuppressWarnings({"unchecked"})
    public DisposeResourcesCommand generate(LogicalComponent<?> component) throws GenerationException {
        if (!(component instanceof LogicalCompositeComponent) || (component.getState() != LogicalState.MARKED)) {
            return null;
        }
        LogicalCompositeComponent composite = (LogicalCompositeComponent) component;
        if (composite.getResources().isEmpty()) {
            return null;
        }
        List<PhysicalResourceDefinition> definitions = new ArrayList<>();
        for (LogicalResource<?> resource : composite.getResources()) {
            Resource resourceDefinition = resource.getDefinition();
            ResourceGenerator generator = generatorRegistry.getResourceGenerator(resourceDefinition.getClass());
            PhysicalResourceDefinition definition = generator.generateResource(resource);
            definitions.add(definition);
        }
        if (definitions.isEmpty()) {
            return null;
        }
        return new DisposeResourcesCommand(definitions);
    }

}