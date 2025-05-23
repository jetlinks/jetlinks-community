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
package org.jetlinks.community.dictionary;

import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.EnumFragmentBuilder;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.EnumInFragmentBuilder;
import org.hswebframework.web.crud.configuration.TableMetadataCustomizer;
import org.jetlinks.community.form.type.EnumFieldType;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.JDBCType;
import java.util.List;
import java.util.Set;

public class DictionaryColumnCustomizer implements TableMetadataCustomizer {
    @Override
    public void customColumn(Class<?> entityType,
                             PropertyDescriptor descriptor,
                             Field field,
                             Set<Annotation> annotations,
                             RDBColumnMetadata column) {
        Dictionary dictionary = annotations
            .stream()
            .filter(Dictionary.class::isInstance)
            .findFirst()
            .map(Dictionary.class::cast)
            .orElse(null);
        if (dictionary != null) {
            Class<?> type = field.getType();

            JDBCType jdbcType = (JDBCType) column.getType().getSqlType();
            EnumFieldType codec = new EnumFieldType(
                type.isArray() || List.class.isAssignableFrom(type),
                dictionary.value(),
                jdbcType)
                .withArray(type.isArray())
                .withFieldValueConverter(e -> e);

            column.setValueCodec(codec);
            if (codec.isToMask()) {
                column.addFeature(EnumFragmentBuilder.eq);
                column.addFeature(EnumFragmentBuilder.not);

                column.addFeature(EnumInFragmentBuilder.of(column.getDialect()));
                column.addFeature(EnumInFragmentBuilder.ofNot(column.getDialect()));

            }
        }
    }

    @Override
    public void customTable(Class<?> entityType, RDBTableMetadata table) {

    }
}
