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
package org.fabric3.fabric.node;

import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.api.model.type.component.Binding;

/**
 * Resolves a service and returns a wire proxy to it.
 */
public interface ServiceResolver {

    /**
     * Resolves the service using autowire and returns a wire proxy to it.
     *
     * @param interfaze the service interface
     * @return the wire proxy
     * @throws Fabric3Exception if there is a resolution exception
     */
    <T> T resolve(Class<T> interfaze) throws Fabric3Exception;

    <T> T resolve(Class<T> interfaze, Binding binding, Class<?> implClass) throws Fabric3Exception;
}
