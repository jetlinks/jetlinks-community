package org.jetlinks.community.device.web.excel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
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
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.core.metadata.unit.ValueUnits;
import org.jetlinks.supports.official.JetLinksDataTypeCodecs;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import javax.validation.constraints.NotBlank;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
@Setter
@Slf4j
public class PropertyMetadataExcelInfo {

    @NotBlank(message = "属性ID不能为空")
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
        ObjectType.ID, StringType.ID, GeoType.ID, FileType.ID, PasswordType.ID, GeoShapeType.ID);

    private static final List<String> OBJECT_NOT_HAVE = Lists.newArrayList(DateTimeType.ID, FileType.ID, ObjectType.ID, PasswordType.ID);
    /**
     * 简单模板支持类型
     */
    private static final List<String> SIMPLE = Lists.newArrayList(IntType.ID, FloatType.ID, DoubleType.ID, LongType.ID);

    public void with(String key, Object value) {
        FastBeanCopier.copy(Collections.singletonMap(key, value), this);
    }

    public PropertyMetadata toMetadata() {
        SimplePropertyMetadata metadata = new SimplePropertyMetadata();
        try {
            ValidatorUtils.tryValidate(this);
            if (CollectionUtils.isEmpty(type) || type.size() == 1 && StringUtils.isEmpty(type.get(0))) {
                throw new ValidationException("读写类型不能为空");
            }
            metadata.setId(property);
            metadata.setName(name);
            metadata.setValueType(parseDataType());
            metadata.setExpands(parseExpands());
            metadata.setDescription(description);
            return metadata;
        } catch (Throwable e) {
            throw new BusinessException("第" + this.getRowNumber() + "行错误：" + e.getMessage());
        }
    }

    public static List<ExcelHeader> getTemplateHeaderMapping(List<ConfigMetadata> configMetadataList) {
        List<ExcelHeader> arr = new ArrayList<>(Arrays.asList(
            new ExcelHeader("property", "属性ID", CellDataType.STRING),
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
            this.dataType = dataTypeJson.getString("type");
        } else {
            dataTypeJson.put("type", this.dataType);
            dataTypeJson.put("unit", this.unit);
            dataTypeJson.put("scale", this.scale);
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
        map.put("source", PropertySource.getValue(source));
        map.put("storageType", PropertyStorage.getValue(storageType));
        map.put("tags", "");
        map.put("type", type.stream().map(PropertyType::getValue).collect(Collectors.toList()));
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
                Random random = new Random();
                excelInfo.setStorageType(random.nextBoolean() ? "direct" : "ignore");
                excelInfo.setSource(random.nextInt(2) == 1 ? "manual" : random.nextInt(2) < 1 ? "device" : "rule");
                excelInfo.setDescription(excelInfo.getName() + "的说明");
                if (SIMPLE.contains(dt)) {
                    excelInfo.setUnit(idList.get(0).getId());
                    excelInfo.setDataType(dt);
                    excelInfo.setScale(String.valueOf(random.nextInt(2)));
                    excelInfo.setDescription(excelInfo.getName() + "的说明,优先使用json数据类型配置，没有则使用简单模板，仅支持int double float long四种");
                }
                Map<String, Object> valueType = JetLinksDataTypeCodecs.encode(buildValueType(dataType, random)).orElse(Collections.emptyMap());
                excelInfo.setValueType(JSONObject.toJSONString(valueType));
                excelInfo.setExpands(Collections.singletonMap("storageType", excelInfo.getStorageType()));
                excelInfo.setType(Arrays.asList("read", "write", "report"));
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
                ((StringType) dataType).expand("maxLength", random.nextInt(2000));
                break;
            case PasswordType.ID:
                ((PasswordType) dataType).expand("maxLength", random.nextInt(30));
                break;
            default:
                break;
        }
        return dataType;
    }


    public Map<String, Object> toMap() {
        setSource(PropertySource.getText(source));
        setStorageType(PropertyStorage.getText(storageType));
        setExpands(Collections.singletonMap("storageType", storageType));
        Map<String, Object> map = FastBeanCopier.copy(this, new HashMap<>(8));
        map.put("type", type.stream()
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
