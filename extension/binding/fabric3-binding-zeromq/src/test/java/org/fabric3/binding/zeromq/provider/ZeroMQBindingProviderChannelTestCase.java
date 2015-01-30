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
package org.fabric3.binding.zeromq.provider;

import java.net.URI;

import junit.framework.TestCase;

import org.fabric3.api.model.type.component.Channel;
import org.fabric3.spi.model.instance.LogicalChannel;

/**
 *
 */
public class ZeroMQBindingProviderChannelTestCase extends TestCase {
    private LogicalChannel channel;
    private ZeroMQBindingProvider provider;

    public void testCanBind() throws Exception {
        assertTrue(provider.canBind(channel).isMatch());
    }

    public void testBindChannel() throws Exception {
        provider.bind(channel);
        assertTrue(!channel.getBindings().isEmpty());
    }

    protected void setUp() {
        provider = new ZeroMQBindingProvider();
        Channel definition = new Channel("channel");
        channel = new LogicalChannel(URI.create("test"), definition, null);
    }


}
