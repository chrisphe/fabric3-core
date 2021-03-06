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
package org.fabric3.implementation.bytecode.proxy.channel;

import org.fabric3.implementation.bytecode.proxy.common.ProxyDispatcher;
import org.fabric3.spi.container.channel.ChannelConnection;
import org.fabric3.spi.container.channel.EventStreamHandler;
import org.fabric3.spi.util.Closeable;

/**
 * Dispatches to an {@link EventStreamHandler}s from a channel proxy based on the index of the proxy method invoked.
 */
public class ChannelProxyDispatcher implements ProxyDispatcher, Closeable {
    private EventStreamHandler handler;
    private Closeable closeable;

    public void init(ChannelConnection connection) {
        this.handler = connection.getEventStream().getHeadHandler();
        this.closeable = connection.getCloseable();
    }

    public Object _f3_invoke(int index, Object param) throws Exception {
        handler.handle(param, true);
        return null;
    }

    public void close() {
        closeable.close();
    }
}
