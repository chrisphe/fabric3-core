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
package org.fabric3.fabric.container.builder.channel;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import org.fabric3.api.model.type.contract.DataType;
import org.fabric3.fabric.model.physical.TypePhysicalEventFilter;
import org.fabric3.spi.model.type.java.JavaType;

/**
 *
 */
public class TypePhysicalEventFilterBuilderTestCase extends TestCase {

    public void testBuild() throws Exception {
        TypeEventFilterBuilder builder = new TypeEventFilterBuilder();
        DataType type = new JavaType(String.class);
        List<DataType> types = new ArrayList<>();
        types.add(type);
        TypePhysicalEventFilter filter = new TypePhysicalEventFilter(types);

        assertNotNull(builder.build(filter));
    }
}