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
import io.netty.util.internal.ThreadLocalRandom;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.reactor.excel.CellDataType;
import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.core.metadata.unit.ValueUnits;
import org.jetlinks.supports.official.JetLinksDataTypeCodecs;
import reactor.core.publisher.Flux;

import jakarta.validation.constraints.NotBlank;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
@Setter
@Slf4j
public class PropertyMetadataExcelInfo {

    @NotBlank(message = "属性标识不能为空")
    private String property;

    @NotBlank(message = "属性名称不能为空")
    private String name;

    private String valueType;

    private Map<String, Object> expands;
    //数据类型
    @NotBlank(message = "数据类型不能为空")
    private String dataType;
    //单位
    private String unit;
    //精度
    private String scale;
    //来源
    @NotBlank(message = "来源不能为空")
    private String source;

    private String description;

    private String storageType;

    private long rowNumber;
    //读写类型
    @NotBlank(message = "读写类型不能为空")
    private String type;

    public static String typeListToString(List<String> type) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < type.size() - 1; i++) {
            sb.append(type.get(i));
            sb.append(",");
        }
        sb.append(type.get(type.size() - 1));
        return sb.toString();
    }

    private List<String> parseType(){
        return Arrays.stream(this.type.split(",")).collect(Collectors.toList());
    }

    /**
     * 单位
     */
    private static final List<ValueUnit> idList = ValueUnits.getAllUnit();

    /**
     * 示例数据类型
     */
    private static final List<String> DATA_TYPES = Lists.newArrayList(BooleanType.ID,
        DateTimeType.ID, DoubleType.ID, EnumType.ID, FloatType.ID, IntType.ID, LongType.ID,
        StringType.ID, GeoType.ID, FileType.ID, PasswordType.ID);

    private static final List<String> OBJECT_NOT_HAVE = Lists.newArrayList(DateTimeType.ID, FileType.ID, ObjectType.ID, PasswordType.ID);
    /**
     * 数字类型
     */
    private static final List<String> numbers = Lists.newArrayList(IntType.ID, FloatType.ID, DoubleType.ID, LongType.ID);


    /**
     * 小数类型
     */
    private static final List<String> decimalNumbers = Lists.newArrayList(FloatType.ID, DoubleType.ID);


    public void with(String key, Object value) {
        FastBeanCopier.copy(Collections.singletonMap(key, value), this);
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
            throw new BusinessException("第" + this.getRowNumber() + "行错误：" + e.getMessage(),e);
        }
    }

    public static List<ExcelHeader> getTemplateHeaderMapping(List<ConfigMetadata> configMetadataList) {
        List<ExcelHeader> arr = new ArrayList<>(Arrays.asList(
            new ExcelHeader("property", "属性标识", CellDataType.STRING),
            new ExcelHeader("name", "属性名称", CellDataType.STRING),
            new ExcelHeader("dataType", "数据类型", CellDataType.STRING),
            new ExcelHeader("unit", "单位", CellDataType.STRING),
            new ExcelHeader("scale", "精度", CellDataType.STRING),
            new ExcelHeader("valueType", "数据类型配置", CellDataType.STRING),
            new ExcelHeader("source", "来源", CellDataType.STRING),
            new ExcelHeader("description", "属性说明", CellDataType.STRING),
            new ExcelHeader("type", "读写类型", CellDataType.STRING)
        ));

        Set<String> expandsKeys = new HashSet<>();
        for (ConfigMetadata configMetadata : configMetadataList) {
            for (ConfigPropertyMetadata property : configMetadata.getProperties()) {
                String header = property.getName();
                if (expandsKeys.contains(header)) {
                    header = configMetadata.getName() + "-" + header;
                }
                arr.add(new ExcelHeader("expands." + property.getProperty(), header, CellDataType.STRING));
                expandsKeys.add(property.getName());
            }
        }

        return arr;
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
        Map<String, Object> map = new HashMap<>(4);
        map.put(DeviceExcelConstants.source, PropertySource.getValue(source));
        Map<String, Object> storageTypeMap = new HashMap<>();
        storageTypeMap.put(DeviceExcelConstants.storageType, PropertyStorage.getValue(storageType));
        expands.put(DeviceExcelConstants.expands, storageTypeMap);
        map.put(DeviceExcelConstants.tags, "");
        map.put(DeviceExcelConstants.type, parseType().stream().map(PropertyType::getValue).collect(Collectors.toList()));
        return map;
    }

    public static Flux<PropertyMetadataExcelInfo> getTemplateContentMapping() {
        return Flux.fromIterable(DATA_TYPES)
            .flatMap(dt -> {
                PropertyMetadataExcelInfo excelInfo = new PropertyMetadataExcelInfo();
                DataType dataType = DataTypes.lookup(dt).get();
                excelInfo.setProperty(dataType.getType() + "_id");
                excelInfo.setName(dataType.getType() + "类型属性示例");
                excelInfo.setDataType(dataType.getId());
                excelInfo.setUnit("");
                excelInfo.setScale("");
                ThreadLocalRandom random = ThreadLocalRandom.current();
                excelInfo.setStorageType(random.nextBoolean() ? PropertyStorage.direct.getText() : PropertyStorage.ignore.getText());
                excelInfo.setSource(PropertySource.device.getText());
                excelInfo.setDescription(excelInfo.getName() + "的说明");
                if (numbers.contains(dt)) {
                    excelInfo.setUnit(idList.get(0).getId());
                    excelInfo.setDataType(dt);
                    excelInfo.setDescription(excelInfo.getName() + "的说明。数据类型配置可以为空");
                    if (decimalNumbers.contains(dt)){
                        excelInfo.setScale(String.valueOf(random.nextInt(2) + 1));
                    }
                }
                Map<String, Object> valueType = JetLinksDataTypeCodecs.encode(buildValueType(dataType, random)).orElse(Collections.emptyMap());
                excelInfo.setValueType(JSONObject.toJSONString(valueType));
                excelInfo.setExpands(Collections.singletonMap(DeviceExcelConstants.storageType, excelInfo.getStorageType()));
                List<String> typeList = Arrays.asList(PropertyType.read.getText(), PropertyType.write.getText(), PropertyType.report.getText());
                excelInfo.setType(typeListToString(typeList));
                return Flux.just(excelInfo);
            }).doOnError(e -> {
                log.error("填充模板异常:", e);
            });
    }

    private static DataType buildValueType(DataType dataType, Random random) {
        switch (dataType.getId()) {
            case ArrayType.ID:
                ((ArrayType) dataType).elementType(new IntType().unit(idList.get(random.nextInt(idList.size() - 1))));
                break;
            case DoubleType.ID:
                ((DoubleType) dataType).scale(random.nextInt(10)).unit(idList.get(random.nextInt(idList.size() - 1)));
                break;
            case FloatType.ID:
                ((FloatType) dataType).scale(random.nextInt(10)).unit(idList.get(random.nextInt(idList.size() - 1)));
                break;
            case EnumType.ID:
                dataType = new EnumType();
                for (int i = 0; i < random.nextInt(5); i++) {
                    ((EnumType) dataType).addElement(EnumType.Element.of("枚举值" + i, String.valueOf(i), "枚举说明" + i));
                }
                break;
            case IntType.ID:
                dataType = new IntType();
                ((IntType) dataType).unit(idList.get(random.nextInt(idList.size() - 1)));
                break;
            case LongType.ID:
                ((LongType) dataType).unit(idList.get(random.nextInt(idList.size() - 1)));
                break;
            case FileType.ID:
                ((FileType) dataType).bodyType(FileType.BodyType.url);
                break;
            case ObjectType.ID:
                int i = 1;
                List<String> objectParam = Lists.newCopyOnWriteArrayList(DATA_TYPES);
                dataType = new ObjectType();
                objectParam.removeAll(OBJECT_NOT_HAVE);
                for (String id : objectParam) {
                    ((ObjectType) dataType).addProperty("param" + i, "参数" + i, buildValueType(DataTypes.lookup(id).get(), random));
                    i++;
                }
                break;
            case StringType.ID:
                ((StringType) dataType).expand(DeviceExcelConstants.maxLength, random.nextInt(2000));
                break;
            case PasswordType.ID:
                ((PasswordType) dataType).expand(DeviceExcelConstants.maxLength, random.nextInt(30));
                break;
            default:
                break;
        }
        return dataType;
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

    public static List<PropertyMetadataExcelInfo> getExcelInfoContent(List<PropertyMetadata> properties) {
        List<PropertyMetadataExcelInfo> excelInfoList = new ArrayList<>();
        for (PropertyMetadata property : properties) {
            PropertyMetadataExcelInfo excelInfo = new PropertyMetadataExcelInfo();
            excelInfo.setProperty(property.getId());
            excelInfo.setName(property.getName());
            excelInfo.setDataType(property.getValueType().getId());
            if (PropertyMetadataExcelInfo.numbers.contains(property.getValueType().getType())) {
                NumberType type = (NumberType) property.getValueType();
                excelInfo.setUnit(type.getUnit() == null ? "" : type.getUnit().getName());
                if (PropertyMetadataExcelInfo.decimalNumbers.contains(property.getValueType().getType())){
                    excelInfo.setScale(type.getScale() == null ? "0" : String.valueOf(type.getScale().intValue()));
                }
            } else {
                excelInfo.setUnit("");
                excelInfo.setScale("");
            }
            property.getValueType();
            Map<String, Object> expands = property.getExpands();
            excelInfo.setSource(String.valueOf(expands.getOrDefault(DeviceExcelConstants.source, "")));
            excelInfo.setStorageType(String.valueOf(expands.getOrDefault(DeviceExcelConstants.storageType, "")));
            excelInfo.setDescription(property.getDescription());
            Map<String, Object> valueType = JetLinksDataTypeCodecs
                .encode(property.getValueType()).orElse(Collections.emptyMap());
            excelInfo.setValueType(JSONObject.toJSONString(valueType));
            excelInfo.setExpands(Collections.singletonMap(DeviceExcelConstants.storageType, excelInfo.getStorageType()));
            excelInfo.setType(property.getExpand(DeviceExcelConstants.type).orElse("").toString());
            excelInfoList.add(excelInfo);
        }
        return excelInfoList;
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
