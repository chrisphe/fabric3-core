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
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 */
package org.fabric3.spi.domain.generator;

import org.fabric3.api.model.type.component.Binding;
import org.fabric3.spi.model.instance.LogicalBinding;

/**
 * Implementations generate a {@link Binding} for a callback. This is required for bindings that do not require a separate callback configuration on a service
 * or reference.
 */
public interface CallbackBindingGenerator<B extends Binding> {

    /**
     * Generates a callback binding definition for a service.
     *
     * @param forwardBinding the forward binding
     * @return the generated binding
     */
    B generateServiceCallback(LogicalBinding<B> forwardBinding);

    /**
     * Generates a callback binding definition for a reference.
     *
     * @param forwardBinding the forward binding
     * @return the binding definition
     */
    B generateReferenceCallback(LogicalBinding<B> forwardBinding);

}