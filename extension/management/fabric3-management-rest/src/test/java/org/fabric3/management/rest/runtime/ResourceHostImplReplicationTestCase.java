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
package org.fabric3.management.rest.runtime;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.fabric3.management.rest.model.ResourceException;
import org.fabric3.management.rest.spi.ResourceMapping;
import org.fabric3.management.rest.spi.Verb;
import org.fabric3.spi.host.ServletHost;

/**
 *
 */
public final class ResourceHostImplReplicationTestCase extends TestCase {
    private ResourceHostImpl host;
    private Method parameterizedMethod;

    public void testDispatch() throws Exception {
        MockResource instance = EasyMock.createMock(MockResource.class);
        EasyMock.expect(instance.parameterized("test")).andReturn("test");
        EasyMock.replay(instance);

        ResourceMapping mapping = new ResourceMapping("foo", "/foo/bar", "bar", Verb.POST, parameterizedMethod, instance, null, null);

        host.start();
        host.register(mapping);

        host.dispatch("/foo/bar", Verb.POST, new String[]{"test"});

        EasyMock.verify(instance);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        parameterizedMethod = MockResource.class.getMethod("parameterized", String.class);

        HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);
        response.setContentType("application/json");

        Marshaller marshaller = EasyMock.createMock(Marshaller.class);
        ServletHost servletHost = EasyMock.createNiceMock(ServletHost.class);
        BasicAuthenticator authenticator = EasyMock.createNiceMock(BasicAuthenticator.class);
        ManagementMonitor monitor = EasyMock.createNiceMock(ManagementMonitor.class);
        host = new ResourceHostImpl(marshaller, servletHost, authenticator, monitor);
    }

    private static interface MockResource {

        void error() throws ResourceException;

        String parameterized(String message);

        void parameterized(HttpServletRequest request);
    }
}
