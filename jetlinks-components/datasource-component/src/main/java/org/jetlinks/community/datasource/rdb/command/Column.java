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
package org.jetlinks.community.datasource.rdb.command;

import lombok.*;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Column {

    private String name;
    private String previousName;

    private String type;

    private boolean primaryKey;
    private int length;
    private int scale;
    private int precision;
    private boolean notnull;

    private String comment;

    public static Column of(RDBColumnMetadata columnMetadata) {
        Column column = new Column();
        column.setPreviousName(columnMetadata.getName());
        column.setName(columnMetadata.getName());
        column.setType(columnMetadata.getType().getSqlType().getName());
        column.setLength(columnMetadata.getLength() == 0 ? columnMetadata.getPrecision() : columnMetadata.getLength());
        column.setScale(columnMetadata.getScale());
        column.setPrecision(columnMetadata.getPrecision());
        column.setNotnull(columnMetadata.isNotNull());
        column.setPrimaryKey(columnMetadata.isPrimaryKey());
        column.setComment(columnMetadata.getComment());
        return column;
    }
}
