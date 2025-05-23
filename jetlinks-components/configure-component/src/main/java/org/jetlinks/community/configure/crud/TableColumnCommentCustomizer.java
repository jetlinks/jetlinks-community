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
package org.jetlinks.community.configure.crud;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.web.crud.configuration.TableMetadataCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;

@Component
public class TableColumnCommentCustomizer implements TableMetadataCustomizer {

    @Override
    public void customColumn(Class<?> entityType,
                             PropertyDescriptor descriptor,
                             Field field,
                             Set<Annotation> annotations,
                             RDBColumnMetadata column) {
        if (StringUtils.isEmpty(column.getComment())) {
            annotations
                .stream()
                .filter(Schema.class::isInstance)
                .map(Schema.class::cast)
                .findAny()
                .ifPresent(schema -> column.setComment(schema.description()));
        }
    }

    @Override
    public void customTable(Class<?> entityType, RDBTableMetadata table) {
        Schema schema = entityType.getAnnotation(Schema.class);
        if (null != schema) {
            table.setComment(schema.description());
        }
    }
}
