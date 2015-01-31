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
package org.fabric3.implementation.pojo.builder;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

import org.fabric3.api.host.runtime.HostInfo;
import org.fabric3.api.model.type.contract.DataType;
import org.fabric3.api.model.type.java.Injectable;
import org.fabric3.api.model.type.java.InjectableType;
import org.fabric3.api.model.type.java.ManagementInfo;
import org.fabric3.implementation.pojo.component.PojoComponent;
import org.fabric3.implementation.pojo.component.PojoComponentContext;
import org.fabric3.implementation.pojo.component.PojoRequestContext;
import org.fabric3.implementation.pojo.manager.ImplementationManagerFactory;
import org.fabric3.implementation.pojo.provision.PojoComponentDefinition;
import org.fabric3.spi.classloader.ClassLoaderRegistry;
import org.fabric3.api.host.ContainerException;
import org.fabric3.spi.container.builder.component.ComponentBuilder;
import org.fabric3.spi.container.component.AtomicComponent;
import org.fabric3.spi.container.component.Component;
import org.fabric3.spi.container.objectfactory.ObjectFactory;
import org.fabric3.spi.container.objectfactory.SingletonObjectFactory;
import org.fabric3.spi.introspection.TypeMapping;
import org.fabric3.spi.introspection.java.IntrospectionHelper;
import org.fabric3.spi.management.ManagementService;
import org.fabric3.spi.model.physical.ParamTypes;
import org.fabric3.spi.model.physical.PhysicalPropertyDefinition;
import org.fabric3.spi.model.type.java.JavaGenericType;
import org.fabric3.spi.model.type.java.JavaType;
import org.fabric3.spi.model.type.java.JavaTypeInfo;
import org.w3c.dom.Document;

/**
 * Base class for component builders that create Java-based components.
 */
public abstract class PojoComponentBuilder<PCD extends PojoComponentDefinition, C extends Component> implements ComponentBuilder<PCD, C> {
    protected ClassLoaderRegistry classLoaderRegistry;
    protected IntrospectionHelper helper;
    private HostInfo info;
    private PropertyObjectFactoryBuilder propertyBuilder;
    private ManagementService managementService;

    protected PojoComponentBuilder(ClassLoaderRegistry registry,
                                   PropertyObjectFactoryBuilder propertyBuilder,
                                   ManagementService managementService,
                                   IntrospectionHelper helper,
                                   HostInfo info) {
        this.classLoaderRegistry = registry;
        this.propertyBuilder = propertyBuilder;
        this.managementService = managementService;
        this.helper = helper;
        this.info = info;
    }

    protected void createPropertyFactories(PCD definition, ImplementationManagerFactory factory) throws ContainerException {
        List<PhysicalPropertyDefinition> propertyDefinitions = definition.getPropertyDefinitions();

        TypeMapping typeMapping = new TypeMapping();
        helper.resolveTypeParameters(factory.getImplementationClass(), typeMapping);

        for (PhysicalPropertyDefinition propertyDefinition : propertyDefinitions) {
            String name = propertyDefinition.getName();
            Injectable source = new Injectable(InjectableType.PROPERTY, name);
            if (propertyDefinition.getInstanceValue() != null) {
                ObjectFactory<Object> objectFactory = new SingletonObjectFactory<>(propertyDefinition.getInstanceValue());
                factory.setObjectFactory(source, objectFactory);
            } else {
                Document value = propertyDefinition.getValue();

                Type type = factory.getGenericType(source);
                DataType dataType = getDataType(type, typeMapping);

                ClassLoader classLoader = classLoaderRegistry.getClassLoader(definition.getClassLoaderId());
                boolean many = propertyDefinition.isMany();
                ObjectFactory<?> objectFactory = propertyBuilder.createFactory(name, dataType, value, many, classLoader);
                factory.setObjectFactory(source, objectFactory);
            }
        }
    }

    protected void export(PojoComponentDefinition definition, ClassLoader classLoader, AtomicComponent component) throws ContainerException {
        if (definition.isManaged()) {
            ManagementInfo info = definition.getManagementInfo();
            ObjectFactory<Object> objectFactory = component.createObjectFactory();
            URI uri = definition.getComponentUri();
            managementService.export(uri, info, objectFactory, classLoader);
        }
    }

    protected void dispose(PojoComponentDefinition definition) throws ContainerException {
        if (definition.isManaged()) {
            ManagementInfo info = definition.getManagementInfo();
            URI uri = definition.getComponentUri();
            managementService.remove(uri, info);
        }
    }

    protected void buildContexts(PojoComponent component, ImplementationManagerFactory factory) {
        PojoRequestContext requestContext = new PojoRequestContext();
        SingletonObjectFactory<PojoRequestContext> requestFactory = new SingletonObjectFactory<>(requestContext);
        factory.setObjectFactory(Injectable.OASIS_REQUEST_CONTEXT, requestFactory);
        PojoComponentContext componentContext = new PojoComponentContext(component, requestContext, info);
        SingletonObjectFactory<PojoComponentContext> componentFactory = new SingletonObjectFactory<>(componentContext);
        factory.setObjectFactory(Injectable.OASIS_COMPONENT_CONTEXT, componentFactory);
    }

    @SuppressWarnings({"unchecked"})
    private DataType getDataType(Type type, TypeMapping typeMapping) {
        if (type instanceof Class) {
            // non-generic type
            Class<?> nonGenericType = (Class<?>) type;
            if (nonGenericType.isPrimitive()) {
                // convert primitive representation to its object equivalent
                nonGenericType = ParamTypes.PRIMITIVE_TO_OBJECT.get(nonGenericType);
            }
            return new JavaType(nonGenericType);
        } else {
            // a generic
            JavaTypeInfo info = helper.createTypeInfo(type, typeMapping);
            return new JavaGenericType(info);

        }
    }

}
