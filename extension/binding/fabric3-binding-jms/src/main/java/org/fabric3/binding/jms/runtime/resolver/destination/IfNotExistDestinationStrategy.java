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
package org.fabric3.binding.jms.runtime.resolver.destination;

import javax.jms.ConnectionFactory;
import java.util.List;
import java.util.Optional;

import org.fabric3.api.annotation.wire.Key;
import org.fabric3.api.binding.jms.model.Destination;
import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.binding.jms.runtime.resolver.DestinationStrategy;
import org.fabric3.binding.jms.spi.runtime.provider.DestinationResolver;
import org.oasisopen.sca.annotation.Reference;

/**
 * Implementation that attempts to resolve a a destination via provider resolvers and, if it is not found, will create it.
 */
@Key("IF_NOT_EXIST")
public class IfNotExistDestinationStrategy implements DestinationStrategy {
    private DestinationStrategy always = new AlwaysDestinationStrategy();
    private List<DestinationResolver> resolvers;

    @Reference(required = false)
    public void setResolvers(List<DestinationResolver> resolvers) {
        this.resolvers = resolvers;
    }

    public javax.jms.Destination getDestination(Destination definition, ConnectionFactory factory) throws Fabric3Exception {
        for (DestinationResolver resolver : resolvers) {
            Optional<javax.jms.Destination> destination = resolver.resolve(definition);
            if (destination.isPresent()) {
                return destination.get();
            }
        }
        return always.getDestination(definition, factory);
    }

}
