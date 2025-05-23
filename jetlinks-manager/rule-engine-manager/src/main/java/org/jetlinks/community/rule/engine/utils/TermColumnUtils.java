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
package org.jetlinks.community.rule.engine.utils;

import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.reactorql.impl.ComplexExistsFunction;
import org.jetlinks.community.rule.engine.scene.DeviceOperation;
import org.jetlinks.community.rule.engine.scene.SceneUtils;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.LongType;
import org.jetlinks.core.metadata.types.ObjectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.jetlinks.core.metadata.SimplePropertyMetadata.of;

/**
 * @author bestfeng
 */
public class TermColumnUtils {

    public static List<TermColumn> parseArrayChildTermColumns(DataType dataType) {
        String prefix = ComplexExistsFunction.COL_ELEMENT;
        if (!(dataType instanceof ArrayType)) {
            return new ArrayList<>();
        }
        ArrayType arrayType = (ArrayType) dataType;
        List<TermColumn> columns = new ArrayList<>();
        if (arrayType.getElementType() instanceof ObjectType) {
            List<PropertyMetadata> properties = ((ObjectType) arrayType.getElementType()).getProperties();
            return TermColumnUtils.createTerm(
                prefix,
                properties,
                (property, column) -> column.setChildren(TermColumnUtils.createTermColumn(prefix, property, true)),
                LocaleUtils.resolveMessage("message.device_metadata_property", "属性"));
        } else {
            SimplePropertyMetadata prop =
                of("this",
                   resolveI18n("message.term_element_of_array",
                               "数组元素"),
                   arrayType.getElementType());
            columns.addAll(TermColumnUtils.createTermColumn(prefix, prop, false));
            // 移除第二层嵌套数组的复杂条件
            if (arrayType.getElementType() instanceof ArrayType) {
                for (TermColumn column : columns) {
                    column.getTermTypes().removeIf(termType -> ComplexExistsFunction.function.equals(termType.getId()));
                }
            }
        }

        return columns;
    }


    public static List<TermColumn> createTermColumn(String prefix,
                                                    PropertyMetadata property,
                                                    boolean last,
                                                    DeviceOperation.PropertyValueType... valueTypes) {
        //对象类型嵌套
        if (property.getValueType() instanceof ObjectType) {
            ObjectType objType = ((ObjectType) property.getValueType());
            if (objType.getProperties().isEmpty()) {
                String _prefix = prefix == null ? property.getId() : prefix + "." + property.getId();
                return createTermColumn0(_prefix, property, last, valueTypes);
            }
            return createTerm(
                objType.getProperties(),
                (prop, column) -> {
                    String _prefix = prefix == null ? property.getId() : prefix + "." + property.getId();
                    if (!last && !(prop.getValueType() instanceof ObjectType)) {
                        TermColumn term = createTermColumn(_prefix, prop, false, valueTypes).get(0);
                        column.setColumn(term.getColumn());
                        column.setName(term.getName());
                        column.setOptions(term.getOptions());
                        column.withOthers(term.getOthers());
                    } else {
                        column.setColumn(SceneUtils.appendColumn(_prefix, prop.getId()));
                        column.setChildren(createTermColumn(_prefix, prop, last, valueTypes));
                    }
                });
        } else if (property.getValueType() instanceof ArrayType) {
            PropertyMetadata sizeMetadata = of("size",
                                               resolveI18n("message.term_size_of_array", "长度"),
                                               new LongType());
            PropertyMetadata valueMetadata = of("this",
                                                resolveI18n("message.term_origin_of_array", "原始值"),
                                                property.getValueType());
            String _prefix = prefix == null ? property.getId() : prefix + "." + property.getId();

            TermColumn size = TermColumn
                .of(SceneUtils.appendColumn(_prefix, sizeMetadata.getId()),
                    sizeMetadata.getName(), sizeMetadata.getValueType())
                .withMetadataTrue();

            size.setChildren(createTermColumn0(_prefix, sizeMetadata, last, valueTypes));

            TermColumn valueThis =
                TermColumn
                    .of(SceneUtils.appendColumn(_prefix, valueMetadata.getId()),
                        valueMetadata.getName(), valueMetadata.getValueType())
                    .withMetadataTrue();
            valueThis.setChildren(createTermColumn0(_prefix, valueMetadata, last, valueTypes));


            return Arrays.asList(size, valueThis);


        } else {
            return createTermColumn0(prefix, property, last, valueTypes);
        }
    }

    public static List<TermColumn> createTerm(List<PropertyMetadata> metadataList,
                                              BiConsumer<PropertyMetadata, TermColumn> consumer,
                                              String... description) {
        List<TermColumn> columns = new ArrayList<>(metadataList.size());
        for (PropertyMetadata metadata : metadataList) {
            TermColumn column = TermColumn.of(metadata);
            column.setDescription(String.join("", description));
            consumer.accept(metadata, column);
            columns.add(column.withMetadataTrue());
        }
        return columns;
    }

    public static List<TermColumn> createTerm(String prefix,
                                              List<PropertyMetadata> metadataList,
                                              BiConsumer<PropertyMetadata, TermColumn> consumer,
                                              String... description) {
        List<TermColumn> columns = new ArrayList<>(metadataList.size());
        for (PropertyMetadata metadata : metadataList) {
            TermColumn column = TermColumn.
                of(SceneUtils.appendColumn(prefix, metadata.getId()),
                   metadata.getName(),
                   metadata.getValueType());
            column.setDescription(String.join("", description));
            consumer.accept(metadata, column);
            columns.add(column.withMetadataTrue());
        }
        return columns;
    }

    private static List<TermColumn> createTermColumn0(String prefix,
                                                      PropertyMetadata property,
                                                      boolean last,
                                                      DeviceOperation.PropertyValueType... valueTypes) {
        if (!last) {
            return Collections.singletonList(
                TermColumn.of(SceneUtils.appendColumn(prefix, property.getId()),
                              property.getName(), property.getValueType())
                          .withMetrics(property)
                          .withMetadataTrue()
            );
        }
        return Arrays
            .stream(valueTypes)
            .map(type -> TermColumn
                .of(SceneUtils
                        .appendColumn(prefix,
                                      property.getId(),
                                      type.name()),
                    type.getKey(),
                    null,
                    type.getDataType() == null ? property.getValueType() : type.getDataType())
                .withMetrics(property)
                .withMetadataTrue()
            )
            .collect(Collectors.toList());
    }

    private static String resolveI18n(String key, String name) {
        return LocaleUtils.resolveMessage(key, name);
    }
}
