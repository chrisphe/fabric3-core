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
package org.fabric3.binding.zeromq.runtime;

import org.fabric3.api.annotation.wire.Key;
import org.fabric3.api.binding.zeromq.model.ZeroMQMetadata;
import org.fabric3.binding.zeromq.provision.ZeroMQConnectionTarget;
import org.fabric3.spi.container.builder.TargetConnectionAttacher;
import org.fabric3.spi.container.channel.ChannelConnection;
import org.fabric3.spi.model.physical.PhysicalConnectionSource;
import org.oasisopen.sca.annotation.Reference;

/**
 *
 */
@Key("org.fabric3.binding.zeromq.provision.ZeroMQConnectionTarget")
public class ZeroMQConnectionTargetAttacher implements TargetConnectionAttacher<ZeroMQConnectionTarget> {
    private ZeroMQPubSubBroker broker;

    public ZeroMQConnectionTargetAttacher(@Reference ZeroMQPubSubBroker broker) {
        this.broker = broker;
    }

    public void attach(PhysicalConnectionSource source, ZeroMQConnectionTarget target, ChannelConnection connection) {
        ZeroMQMetadata metadata = target.getMetadata();
        String connectionId = source.getUri().toString();
        ClassLoader loader = target.getClassLoader();
        boolean dedicatedThread = target.isDedicatedThread();
        broker.connect(connectionId, metadata, dedicatedThread, connection, loader);
    }

    public void detach(PhysicalConnectionSource source, ZeroMQConnectionTarget target) {
        ZeroMQMetadata metadata = target.getMetadata();
        String connectionId = source.getUri().toString();
        broker.release(connectionId, metadata);
    }

}
