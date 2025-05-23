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
package org.jetlinks.community.device.web.excel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.core.metadata.unit.ValueUnits;
import org.jetlinks.supports.official.JetLinksDataTypeCodecs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
@Setter
@Slf4j
public class PropertyMetadataExcelImportInfo {

    @Pattern(regexp = "^[a-zA-Z0-9_-]+$",message = "error.import_property_metadata_id_validate_error")
    @NotBlank(message = "error.import_property_metadata_id_not_null")
    private String property;

    @NotBlank(message = "error.import_property_metadata_name_not_null")
    private String name;

    private String valueType;

    private Map<String, Object> expands = new HashMap<>();

    //数据类型
    @NotBlank(message = "error.import_property_metadata_data_type_not_null")
    private String dataType;
    //单位
    private String unit;
    //精度
    private String scale;
    //来源
    @NotBlank(message = "error.import_property_metadata_source_not_null")
    private String source;

    private String description;

    private String storageType;

    private long rowNumber;
    //读写类型
    @NotBlank(message = "error.import_property_metadata_type_not_null")
    private String type;

    private List<String> parseType(){
        return Arrays.stream(this.type.split(",")).collect(Collectors.toList());
    }

    /**
     * 单位
     */
    private static final List<ValueUnit> idList = ValueUnits.getAllUnit();

    /**
     * 所有数据类型
     */
    private static final List<String> DATA_TYPES = Lists.newArrayList(ArrayType.ID, BooleanType.ID,
        DateTimeType.ID, DoubleType.ID, EnumType.ID, FloatType.ID, IntType.ID, LongType.ID,
        ObjectType.ID, StringType.ID, GeoType.ID, FileType.ID, PasswordType.ID);

    private static final List<String> OBJECT_NOT_HAVE = Lists.newArrayList(DateTimeType.ID, FileType.ID, ObjectType.ID, PasswordType.ID);
    /**
     * 简单模板支持类型
     */
    private static final List<String> SIMPLE = Lists.newArrayList(IntType.ID, FloatType.ID, DoubleType.ID, LongType.ID);

    public void with(String key, Object value) {
        FastBeanCopier.copy(Collections.singletonMap(key, value), this);
    }

    public void withExpands(String key, Object value) {
        FastBeanCopier.copy(Collections.singletonMap(key, value), expands);
    }

    public PropertyMetadata toMetadata() {
        SimplePropertyMetadata metadata = new SimplePropertyMetadata();
        try {
            ValidatorUtils.tryValidate(this);
            metadata.setId(property);
            metadata.setName(name);
            metadata.setValueType(parseDataType());
            metadata.setExpands(parseExpands());
            metadata.setDescription(description);
            return metadata;
        } catch (Throwable e) {
            throw new BusinessException.NoStackTrace("error.import_property_metadata_error_index",
                                                     400,
                                                     getRowNumber(),
                                                     e.getLocalizedMessage());
        }
    }

    protected DataType parseDataType() {
        JSONObject dataTypeJson = new JSONObject();
        //默认先使用json格式的数据解析物模型，没有json则使用简单模板，只支持int long double float
        if (!StringUtils.isEmpty(this.valueType)) {
            dataTypeJson = JSON.parseObject(this.valueType);
            this.dataType = dataTypeJson.getString(DeviceExcelConstants.type);
        } else {
            dataTypeJson.put(DeviceExcelConstants.type, this.dataType);
            dataTypeJson.put(DeviceExcelConstants.unit, this.unit);
            dataTypeJson.put(DeviceExcelConstants.scale, this.scale);
        }
        DataType dataType = Optional.ofNullable(this.dataType)
            .map(DataTypes::lookup)
            .map(Supplier::get)
            .orElseThrow(() -> new BusinessException("error.unknown_data_type" ,500, this, getDataType()));
        JSONObject finalDataTypeJson = dataTypeJson;
        JetLinksDataTypeCodecs
            .getCodec(dataType.getId())
            .ifPresent(codec -> codec.decode(dataType, finalDataTypeJson));
        return dataType;
    }

    protected Map<String, Object> parseExpands() {
        // 处理系统默认的扩展信息（中文转换），合并到导入模板的expands中
        expands.put(DeviceExcelConstants.source, PropertySource.getValue(source));
        Map<String, Object> storageTypeMap = new HashMap<>();
        storageTypeMap.put(DeviceExcelConstants.storageType, PropertyStorage.getValue(storageType));
        expands.put(DeviceExcelConstants.expands, storageTypeMap);
        expands.put(DeviceExcelConstants.tags, "");
        expands.put(DeviceExcelConstants.type, parseType().stream().map(PropertyType::getValue).collect(Collectors.toList()));
        return expands;
    }


    public Map<String, Object> toMap() {
        setSource(PropertySource.getText(source));
        setStorageType(PropertyStorage.getText(storageType));
        setExpands(Collections.singletonMap(DeviceExcelConstants.storageType, storageType));
        Map<String, Object> map = FastBeanCopier.copy(this, new HashMap<>(8));
        map.put(DeviceExcelConstants.type, parseType()
            .stream()
            .map(PropertyType::getText)
            .collect(Collectors.joining(",")));
        return map;
    }

    @AllArgsConstructor
    @Getter
    private enum PropertySource implements EnumDict<String> {
        device("设备"),
        manual("手动"),
        rule("规则");

        private String text;

        @Override
        public String getValue() {
            return name();
        }

        public static String getText(String value) {
            return EnumDict.findByValue(PropertySource.class, value).map(PropertySource::getText).orElse("");
        }

        public static String getValue(String text) {
            return EnumDict.findByText(PropertySource.class, text).map(PropertySource::getValue).orElse("");
        }
    }

    @AllArgsConstructor
    @Getter
    private enum PropertyType implements EnumDict<String> {
        read("读"),
        write("写"),
        report("上报");

        private String text;

        @Override
        public String getValue() {
            return name();
        }

        public static String getText(String value) {
            return EnumDict.findByValue(PropertyType.class, value).map(PropertyType::getText).orElse("");
        }

        public static String getValue(String text) {
            return EnumDict.findByText(PropertyType.class, text).map(PropertyType::getValue).orElse("");
        }
    }

    @AllArgsConstructor
    @Getter
    private enum PropertyStorage implements EnumDict<String> {
        direct("存储"),
        ignore("不存储");

        private String text;

        @Override
        public String getValue() {
            return name();
        }

        public static String getText(String value) {
            return EnumDict.findByValue(PropertyStorage.class, value).map(PropertyStorage::getText).orElse("");
        }

        public static String getValue(String text) {
            return EnumDict.findByText(PropertyStorage.class, text).map(PropertyStorage::getValue).orElse("");
        }
    }
}
