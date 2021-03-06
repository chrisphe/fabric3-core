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
package org.fabric3.implementation.reflection.jdk;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

import org.fabric3.api.host.Fabric3Exception;

/**
 * Reflectively instantiates a Java-based component instance.
 */
public class ReflectiveSupplier<T> implements Supplier<T> {
    private final Constructor<T> constructor;
    private final Supplier<?>[] paramFactories;

    /**
     * Constructor.
     *
     * @param constructor    the constructor to use for instance instantiation
     * @param paramFactories factories for creating constructor parameters
     */
    public ReflectiveSupplier(Constructor<T> constructor, Supplier<?>[] paramFactories) {
        this.constructor = constructor;
        this.paramFactories = paramFactories;
    }

    public T get() throws Fabric3Exception {
        try {
            if (paramFactories == null) {
                return constructor.newInstance();
            } else {
                Object[] params = new Object[paramFactories.length];
                for (int i = 0; i < paramFactories.length; i++) {
                    Supplier<?> paramFactory = paramFactories[i];
                    params[i] = paramFactory.get();
                }
                try {
                    return constructor.newInstance(params);
                } catch (IllegalArgumentException e) {
                    // check which of the parameters could not be assigned
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    String name = constructor.toString();
                    for (int i = 0; i < paramTypes.length; i++) {
                        Class<?> paramType = paramTypes[i];
                        if (paramType.isPrimitive() && params[i] == null) {
                            throw new Fabric3Exception("Cannot assign null value to primitive for parameter " + i + " of " + name);
                        }
                        if (params[i] != null && !paramType.isInstance(params[i])) {
                            throw new Fabric3Exception(
                                    "Unable to assign parameter of type " + params[i].getClass().getName() + " to parameter " + i + " of " + name);
                        }
                    }
                    // did not fail because of incompatible assignment
                    throw new Fabric3Exception(name, e);
                }
            }
        } catch (InstantiationException e) {
            String name = constructor.getDeclaringClass().getName();
            throw new AssertionError("Class is not instantiable:" + name);
        } catch (IllegalAccessException e) {
            String id = constructor.toString();
            throw new AssertionError("Constructor is not accessible: " + id);
        } catch (InvocationTargetException e) {
            String id = constructor.toString();
            throw new Fabric3Exception("Exception thrown by constructor: " + id, e.getCause());
        }
    }
}
