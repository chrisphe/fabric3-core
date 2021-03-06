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
package org.fabric3.implementation.pojo.spi.proxy;

import java.net.URI;
import java.util.function.Supplier;

import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.spi.container.wire.Wire;

/**
 * Delegates to a {@link WireProxyServiceExtension} to create wire proxies.
 */

public interface WireProxyService {
    /**
     * Creates a Supplier that provides proxies for the forward wire.
     *
     * @param interfaze   the interface the proxy implements
     * @param wire        the wire to proxy @return a Supplier that will create proxies
     * @param callbackUri the callback URI or null if the wire is unidirectional
     * @return the factory
     * @throws Fabric3Exception if there was a problem creating the proxy
     */
    <T> Supplier<T> createSupplier(Class<T> interfaze, Wire wire, String callbackUri) throws Fabric3Exception;

    /**
     * Creates a Supplier that provides proxies for the callback wire.
     *
     * @param interfaze     the interface the proxy implements
     * @param multiThreaded if the proxy should be thread-safe
     * @param callbackUri   the callback service uri
     * @param wire          the wire to proxy
     * @return a Supplier that will create proxies
     * @throws Fabric3Exception if there was a problem creating the proxy
     */
    <T> Supplier<T> createCallbackSupplier(Class<T> interfaze, boolean multiThreaded, URI callbackUri, Wire wire) throws Fabric3Exception;

    /**
     * Updates a Supplier with an additional callback wire. This is used when multiple clients are wired to a target bidirectional service.
     *
     * @param supplier      the Supplier to update
     * @param interfaze     the interface the proxy implements
     * @param multiThreaded if the proxy should be thread-safe
     * @param callbackUri   the callback service uri
     * @param wire          the wire to proxy
     * @return a Supplier that will create proxies
     * @throws Fabric3Exception if there was a problem creating the proxy
     */
    <T> Supplier<?> updateCallbackSupplier(Supplier<?> supplier, Class<T> interfaze, boolean multiThreaded, URI callbackUri, Wire wire) throws Fabric3Exception;

}
