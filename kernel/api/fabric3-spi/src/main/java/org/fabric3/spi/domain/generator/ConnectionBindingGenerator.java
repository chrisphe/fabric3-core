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
package org.fabric3.spi.domain.generator;

import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.api.model.type.component.Binding;
import org.fabric3.spi.model.instance.LogicalBinding;
import org.fabric3.spi.model.instance.LogicalConsumer;
import org.fabric3.spi.model.instance.LogicalProducer;
import org.fabric3.spi.model.physical.DeliveryType;
import org.fabric3.spi.model.physical.PhysicalConnectionSource;
import org.fabric3.spi.model.physical.PhysicalConnectionTarget;

/**
 * Generates {@link PhysicalConnectionSource}s and {@link PhysicalConnectionTarget}s for resolved connection bindings.
 */
public interface ConnectionBindingGenerator<B extends Binding> {

    /**
     * Generates metadata used to attach a consumer to a channel transport.
     *
     * @param consumer     rhe consumer
     * @param binding      the channel binding configuration
     * @param deliveryType the channel delivery semantics
     * @return the connection metadata
     * @throws Fabric3Exception if an error occurs during the generation process
     */
    PhysicalConnectionSource generateConnectionSource(LogicalConsumer consumer, LogicalBinding<B> binding, DeliveryType deliveryType) throws Fabric3Exception;

    /**
     * Generates metadata used to attach a producer to a channel transport.
     *
     * @param producer     the producer
     * @param binding      the channel binding configuration
     * @param deliveryType the channel delivery semantics
     * @return the connection metadata
     * @throws Fabric3Exception if an error occurs during the generation process
     */
    PhysicalConnectionTarget generateConnectionTarget(LogicalProducer producer, LogicalBinding<B> binding, DeliveryType deliveryType) throws Fabric3Exception;

}