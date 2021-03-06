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
package org.fabric3.implementation.java.introspection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.fabric3.api.model.type.component.Component;
import org.fabric3.api.model.type.component.ComponentType;
import org.fabric3.api.model.type.component.Consumer;
import org.fabric3.api.model.type.component.Scope;
import org.fabric3.api.model.type.component.Service;
import org.fabric3.api.model.type.contract.ServiceContract;
import org.fabric3.api.model.type.java.InjectingComponentType;
import org.fabric3.api.model.type.java.JavaImplementation;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.TypeMapping;
import org.fabric3.spi.introspection.java.ImplementationProcessor;
import org.fabric3.spi.introspection.java.IntrospectionHelper;
import org.fabric3.spi.introspection.java.PostProcessor;
import org.fabric3.spi.introspection.java.annotation.AnnotationProcessor;
import org.fabric3.spi.introspection.java.contract.JavaContractProcessor;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Reference;

/**
 * Adds metadata for Java component implementations.
 */
@EagerInit
public abstract class AbstractPojoImplementationProcessor implements ImplementationProcessor<JavaImplementation> {
    private JavaContractProcessor contractProcessor;
    private JavaImplementationIntrospector introspector;
    private IntrospectionHelper helper;
    private Map<Class<? extends Annotation>, AnnotationProcessor<? extends Annotation>> annotationProcessors;
    private List<PostProcessor> postProcessors = Collections.emptyList();

    @Reference(required = false)
    public void setPostProcessors(List<PostProcessor> postProcessors) {
        this.postProcessors = postProcessors;
    }

    @Reference
    public void setAnnotationProcessors(Map<Class<? extends Annotation>, AnnotationProcessor<? extends Annotation>> processors) {
        this.annotationProcessors = processors;
    }

    public AbstractPojoImplementationProcessor(@Reference JavaContractProcessor contractProcessor,
                                               @Reference JavaImplementationIntrospector introspector,
                                               @Reference IntrospectionHelper helper) {
        this.contractProcessor = contractProcessor;
        this.introspector = introspector;
        this.helper = helper;
    }

    public void process(Component<JavaImplementation> component, IntrospectionContext context) {
        JavaImplementation implementation = component.getImplementation();
        Object instance = implementation.getInstance();
        InjectingComponentType componentType = implementation.getComponentType();
        if (instance == null) {
            introspector.introspect(componentType, context);
        } else {
            componentType.setScope(Scope.COMPOSITE);

            if (componentType.getServices().isEmpty()) {
                // introspect services if not defined
                addServiceDefinitions(instance, componentType, context);
            }
            processAnnotations(instance, component, context);

            for (PostProcessor postProcessor : postProcessors) {
                postProcessor.process(componentType, instance.getClass(), context);
            }
        }
    }

    public void process(Component<JavaImplementation> component, Class<?> clazz, IntrospectionContext context) {
        JavaImplementation implementation = createImplementation(clazz, context);
        component.setImplementation(implementation);
        process(component, context);
    }

    protected abstract JavaImplementation createImplementation(Class<?> clazz, IntrospectionContext context);

    @SuppressWarnings("unchecked")
    private void processAnnotations(Object instance, Component<?> definition, IntrospectionContext context) {
        InjectingComponentType componentType = (InjectingComponentType) definition.getComponentType();
        Class<?> implClass = instance.getClass();
        // handle consumer annotations
        AnnotationProcessor consumerProcessor = annotationProcessors.get(org.fabric3.api.annotation.Consumer.class);
        for (Method method : implClass.getDeclaredMethods()) {
            org.fabric3.api.annotation.Consumer consumer = method.getAnnotation(org.fabric3.api.annotation.Consumer.class);
            if (consumer == null) {
                continue;
            }
            TypeMapping mapping = context.getTypeMapping(implClass);
            if (mapping == null) {
                mapping = new TypeMapping();
                context.addTypeMapping(implClass, mapping);
            }

            helper.resolveTypeParameters(implClass, mapping);

            consumerProcessor.visitMethod(consumer, method, implClass, componentType, context);
        }
        // add automatic configuration for consumer annotations
        for (Consumer<ComponentType> consumer : componentType.getConsumers().values()) {
            String name = consumer.getName();
            Consumer<Component> componentConsumer = new Consumer<>(name);
            componentConsumer.setSources(Collections.singletonList(URI.create(name)));
            definition.add(componentConsumer);
        }
    }

    private void addServiceDefinitions(Object instance, InjectingComponentType componentType, IntrospectionContext context) {
        Class<?> implClass = instance.getClass();
        Class[] interfaces = implClass.getInterfaces();
        Class<?> serviceInterface;
        if (interfaces.length == 0) {
            serviceInterface = implClass;
        } else if (interfaces.length == 1) {
            serviceInterface = interfaces[0];
        } else {
            MultipleInterfacesSupported failure = new MultipleInterfacesSupported(implClass, componentType);
            context.addError(failure);
            return;
        }

        String serviceName = serviceInterface.getSimpleName();
        ServiceContract contract = contractProcessor.introspect(serviceInterface, context);
        Service<ComponentType> service = new Service<>(serviceName, contract);
        componentType.add(service);
    }
}
