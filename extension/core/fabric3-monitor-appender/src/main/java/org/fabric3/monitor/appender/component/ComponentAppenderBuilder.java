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
 */
package org.fabric3.monitor.appender.component;

import java.net.URI;
import java.nio.ByteBuffer;

import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.api.host.Names;
import org.fabric3.monitor.spi.appender.Appender;
import org.fabric3.monitor.spi.appender.AppenderBuilder;
import org.fabric3.spi.container.component.AtomicComponent;
import org.fabric3.spi.container.component.Component;
import org.fabric3.spi.container.component.ComponentManager;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Reference;

/**
 * Instantiates a component based appender from a {@link PhysicalComponentAppender}.
 */
@EagerInit
public class ComponentAppenderBuilder implements AppenderBuilder<PhysicalComponentAppender> {
    private ComponentManager componentManager;

    public ComponentAppenderBuilder(@Reference ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    public Appender build(PhysicalComponentAppender definition) throws Fabric3Exception {
        URI uri = URI.create(Names.RUNTIME_NAME + "/" + definition.getComponentName());

        Component component = componentManager.getComponent(uri);
        if (component == null) {
            throw new Fabric3Exception("Component not found: " + uri);
        }
        if (!(component instanceof AtomicComponent)) {
            throw new Fabric3Exception("Component must be atomic: " + uri);
        }
        AtomicComponent atomicComponent = (AtomicComponent) component;
        return new AppenderAdapter(atomicComponent);
    }

    /**
     * Wraps the appender to allow lazy-initialization.
     */
    private class AppenderAdapter implements Appender {
        private AtomicComponent atomicComponent;
        private Appender delegate;

        private AppenderAdapter(AtomicComponent atomicComponent) {
            this.atomicComponent = atomicComponent;
        }

        public void start() {
            Object instance = atomicComponent.getInstance();
            if (!(instance instanceof Appender)) {
                throw new Fabric3Exception("Component does not implement " + Appender.class.getName() + ": " + atomicComponent.getUri());
            }
            delegate = (Appender) instance;
        }

        public void stop() {
        }

        public void write(ByteBuffer buffer) {
            delegate.write(buffer);
        }
    }
}
