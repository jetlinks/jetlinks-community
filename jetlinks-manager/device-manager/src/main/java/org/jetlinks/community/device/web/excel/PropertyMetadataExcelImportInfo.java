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
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.core.metadata.unit.ValueUnits;
import org.jetlinks.supports.official.JetLinksDataTypeCodecs;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
@Setter
@Slf4j
public class PropertyMetadataExcelImportInfo {

    private String property;

    private String name;

    private String valueType;

    private Map<String, Object> expands = new HashMap<>();
    //数据类型
    private String dataType;
    //单位
    private String unit;
    //精度
    private String scale;
    //来源
    private String source;

    private String description;

    private String storageType;

    private long rowNumber;
    //读写类型
    private List<String> type;

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
            metadata.setId(property);
            metadata.setName(name);
            metadata.setValueType(parseDataType());
            metadata.setExpands(parseExpands());
            metadata.setDescription(description);
            return metadata;
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
        expands.put(DeviceExcelConstants.storageType, PropertyStorage.getValue(storageType));
        expands.put(DeviceExcelConstants.tags, "");
        expands.put(DeviceExcelConstants.type, type.stream().map(PropertyType::getValue).collect(Collectors.toList()));
        return expands;
    }


    public Map<String, Object> toMap() {
        setSource(PropertySource.getText(source));
        setStorageType(PropertyStorage.getText(storageType));
        setExpands(Collections.singletonMap(DeviceExcelConstants.storageType, storageType));
        Map<String, Object> map = FastBeanCopier.copy(this, new HashMap<>(8));
        map.put(DeviceExcelConstants.type, type.stream()
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
