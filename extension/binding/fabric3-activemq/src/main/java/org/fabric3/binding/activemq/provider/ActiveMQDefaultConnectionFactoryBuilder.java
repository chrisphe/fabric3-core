package org.fabric3.binding.activemq.provider;

import java.net.URI;

import org.fabric3.api.annotation.wire.Key;
import org.fabric3.api.binding.jms.resource.ConnectionFactoryConfiguration;
import org.fabric3.api.binding.jms.resource.ConnectionFactoryType;
import org.fabric3.binding.jms.spi.runtime.provider.DefaultConnectionFactoryBuilder;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Reference;

/**
 * Creates default connection factory configurations for ActiveMQ.
 */
@EagerInit
@Key("activemq")
public class ActiveMQDefaultConnectionFactoryBuilder implements DefaultConnectionFactoryBuilder {
    private URI defaultBrokerName;

    public ActiveMQDefaultConnectionFactoryBuilder(@Reference BrokerHelper helper) {
        this.defaultBrokerName = URI.create(helper.getDefaultBrokerName());
    }

    public ConnectionFactoryConfiguration createDefaultFactory(String name, ConnectionFactoryType type) {
        ConnectionFactoryConfiguration configuration = new ConnectionFactoryConfiguration(name, "activemq");
        configuration.addAttribute("broker.uri", defaultBrokerName);
        configuration.setType(type);
        return configuration;
    }
}
