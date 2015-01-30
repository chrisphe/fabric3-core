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
package org.fabric3.binding.ws.metro.util;

import org.fabric3.spi.classloader.MultiParentClassLoader;

/**
 *
 */
public class ClassLoaderUpdaterImpl implements ClassLoaderUpdater {

    public void updateClassLoader(Class<?> seiClass) {
        ClassLoader seiClassLoader = seiClass.getClassLoader();
        ClassLoader extensionClassLoader = getClass().getClassLoader();
        if (seiClassLoader instanceof MultiParentClassLoader) {
            MultiParentClassLoader multiParentClassLoader = (MultiParentClassLoader) seiClassLoader;
            if (!multiParentClassLoader.getParents().contains(extensionClassLoader)) {
                multiParentClassLoader.addParent(extensionClassLoader);
            }
        }
    }
}