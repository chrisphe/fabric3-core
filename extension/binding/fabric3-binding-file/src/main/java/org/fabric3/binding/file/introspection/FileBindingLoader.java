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
package org.fabric3.binding.file.introspection;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.fabric3.api.Namespaces;
import org.fabric3.api.annotation.wire.Key;
import org.fabric3.api.binding.file.annotation.Strategy;
import org.fabric3.api.binding.file.model.FileBinding;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.xml.AbstractValidatingTypeLoader;
import org.fabric3.spi.introspection.xml.InvalidValue;
import org.fabric3.spi.introspection.xml.LoaderUtil;
import org.fabric3.spi.introspection.xml.MissingAttribute;
import org.oasisopen.sca.annotation.EagerInit;

/**
 * Loads a <code>binding.file</code> element in a composite.
 */
@EagerInit
@Key(Namespaces.F3_PREFIX + "binding.file")
public class FileBindingLoader extends AbstractValidatingTypeLoader<FileBinding> {

    public FileBindingLoader() {
        addAttributes("requires",
                      "location",
                      "archive.location",
                      "error.location",
                      "strategy",
                      "pattern",
                      "name",
                      "adapter",
                      "adapter.component",
                      "policySets",
                      "delay");
    }

    public FileBinding load(XMLStreamReader reader, IntrospectionContext context) throws XMLStreamException {
        Location startLocation = reader.getLocation();

        String bindingName = reader.getAttributeValue(null, "name");

        String location = reader.getAttributeValue(null, "location");
        if (location == null) {
            MissingAttribute error = new MissingAttribute("The location attribute must be specified", startLocation);
            context.addError(error);
        }

        String archiveLocation = reader.getAttributeValue(null, "archive.location");

        Strategy strategy = parseStrategy(reader);
        if (Strategy.ARCHIVE == strategy && archiveLocation == null) {
            MissingAttribute error = new MissingAttribute("An archive location must be specified", startLocation);
            context.addError(error);
        }
        String errorLocation = reader.getAttributeValue(null, "error.location");

        String adapterClass = reader.getAttributeValue(null, "adapter");

        String adapterUri = reader.getAttributeValue(null, "adapter.component");

        String pattern = reader.getAttributeValue(null, "pattern");

        long delay = parseDelay(reader, context);
        FileBinding binding = new FileBinding(bindingName, pattern, location, strategy, archiveLocation, errorLocation, adapterClass, adapterUri, delay);

        validateAttributes(reader, context, binding);

        LoaderUtil.skipToEndElement(reader);
        return binding;
    }

    private Strategy parseStrategy(XMLStreamReader reader) {
        String strategyAttr = reader.getAttributeValue(null, "strategy");
        if (strategyAttr == null || Strategy.DELETE.toString().toUpperCase().equals(strategyAttr)) {
            return Strategy.DELETE;
        } else {
            return Strategy.valueOf(strategyAttr.toUpperCase());
        }
    }

    private long parseDelay(XMLStreamReader reader, IntrospectionContext context) {
        long delay = -1;
        String delayStr = reader.getAttributeValue(null, "delay");
        if (delayStr != null) {
            try {
                delay = Long.parseLong(delayStr);
            } catch (NumberFormatException e) {
                Location location = reader.getLocation();
                InvalidValue error = new InvalidValue("Invalid delay value", location, e);
                context.addError(error);
            }
        }
        return delay;
    }

}
