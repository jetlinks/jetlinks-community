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
package org.jetlinks.community.device.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.dict.I18nEnumDict;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.Metadata;
import org.jetlinks.core.metadata.types.*;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author: wangsheng
 */
@Getter
@AllArgsConstructor
public enum ValidateDataType implements I18nEnumDict<String> {
    type_int("int", "整数类型", IntType.class) {
        @Override
        public Mono<Void> validate(DataType valueType, String property) {
            return Mono.empty();
        }
    },

    type_long("long", "长整类型", LongType.class) {
        @Override
        public Mono<Void> validate(DataType valueType, String property) {
            return Mono.empty();
        }
    },

    type_float("float", "浮点类型", FloatType.class) {
        @Override
        public Mono<Void> validate(DataType valueType, String property) {
            return Mono.empty();
        }
    },

    type_double("double", "双精度浮点类型", DoubleType.class) {
        @Override
        public Mono<Void> validate(DataType valueType, String property) {
            return Mono.empty();
        }
    },

    type_string("string", "字符串类型", StringType.class) {
        @Override
        public Mono<Void> validate(DataType valueType, String property) {
            return Mono.empty();
        }
    },

    type_boolean("boolean", "布尔类型", BooleanType.class) {
        @Override
        public Mono<Void> validate(DataType valueType, String property) {
            return Mono.empty();
        }
    },

    type_date("date", "日期类型", DateTimeType.class) {
        @Override
        public Mono<Void> validate(DataType valueType, String property) {
            DateTimeType type = (DateTimeType) valueType;
            if (!StringUtils.hasText(type.getFormat())) {
                return Mono.error(new ValidationException.NoStackTrace("error.property_metadata_time_type_format_can_not_be_null", "", property));
            }
            return Mono.empty();
        }
    },

    type_enum("enum", "枚举类型", EnumType.class) {
        @Override
        public Mono<Void> validate(DataType valueType, String property) {
            EnumType type = (EnumType) valueType;
            List<EnumType.Element> elements = type.getElements();
            if (CollectionUtils.isEmpty(elements)) {
                return Mono.error(new ValidationException.NoStackTrace("error.property_metadata_enum_type_can_not_be_null", "", property));
            }
            for (EnumType.Element element : elements) {
                if (!StringUtils.hasText(element.getValue()) && !StringUtils.hasText(element.getValue())) {
                    return Mono.error(new ValidationException.NoStackTrace("error.property_metadata_enum_type_value_can_not_be_null", "", property));
                }
            }
            return Mono.empty();
        }
    },

    type_array("array", "数组类型", ArrayType.class) {
        @Override
        Mono<Void> validate(DataType valueType, String property) {
            ArrayType type = (ArrayType) valueType;
            return handleValidateDataType(type.getElementType(), property);
        }
    },

    type_object("object", "对象类型", ObjectType.class) {
        @Override
        Mono<Void> validate(DataType valueType, String property) {
            ObjectType type = (ObjectType) valueType;
            return Flux
                .fromIterable(type.getProperties())
                .flatMap(prop -> validateIdAndName(prop)
                    .then(handleValidateDataType(prop.getValueType(), property)))
                .then();
        }
    },

    type_file("file", "文件类型", FileType.class) {
        @Override
        public Mono<Void> validate(DataType valueType, String property) {
            return Mono.empty();
        }
    },

    type_password("password", "密码类型", PasswordType.class) {
        @Override
        public Mono<Void> validate(DataType valueType, String property) {
            return Mono.empty();
        }
    },

    type_geoPoint("geoPoint", "地理位置", GeoType.class) {
        @Override
        public Mono<Void> validate(DataType valueType, String property) {
            return Mono.empty();
        }
    },
    ;

    private final String code;
    private final String text;
    private final Class<? extends DataType> type;

    @Override
    public String getValue() {
        return getCode();
    }


    abstract Mono<Void> validate(DataType valueType, String property);

    public static Mono<Void> validateIdAndName(Metadata metadata) {
        if (!StringUtils.hasText(metadata.getId())) {
            return Mono.error(new ValidationException.NoStackTrace("error.property_metadata_id_can_not_be_null","", metadata.getName()));
        }

        if (!StringUtils.hasText(metadata.getName())) {
            return Mono.error(new ValidationException.NoStackTrace("error.property_metadata_name_can_not_be_null", "", metadata.getId()));
        }
        return Mono.empty();
    }

    public static Mono<Void> handleValidateDataType(DataType valueType, String property) {
        if (valueType == null) {
            return Mono.error(new ValidationException.NoStackTrace("error.property_metadata_type_can_not_be_null", "", property));
        }

        ValidateDataType type = EnumDict.findByValue(ValidateDataType.class, valueType.getType()).orElse(null);
        if (type == null) {
            return Mono.error(new ValidationException.NoStackTrace("error.property_metadata_type_error", "", property));
        }
        return type.validate(valueType, property);
    }
}
