/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.iotdb;

import java.util.Objects;

public final class SchemaColumn {

    private final String name;
    private final String type;
    private final boolean tag;

    public SchemaColumn(String name, String type, boolean tag) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isTag() {
        return tag;
    }
}
