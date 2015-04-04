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
package org.fabric3.node.nonmanaged;

import java.util.function.Consumer;

import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.spi.container.builder.component.TargetConnectionAttacher;
import org.fabric3.spi.container.channel.ChannelConnection;
import org.fabric3.spi.model.physical.PhysicalConnectionSource;
import org.fabric3.spi.util.Cast;

/**
 *
 */
public class NonManagedConnectionTargetAttacher implements TargetConnectionAttacher<NonManagedConnectionTarget> {

    public void attach(PhysicalConnectionSource source, NonManagedConnectionTarget target, ChannelConnection connection) {
        if (target.isDirectConnection()) {
            target.setCloseable(connection.getCloseable());
            target.setProxy(connection.getDirectConnection().get().get());
        } else {
            Consumer<Object> consumer = Cast.cast(target.getConsumer());
            connection.getEventStream().addHandler((event, endOfBatch) -> consumer.accept(event));
        }
    }

    public void detach(PhysicalConnectionSource source, NonManagedConnectionTarget target) throws Fabric3Exception {
        // no-op
    }

}