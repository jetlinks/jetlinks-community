package org.jetlinks.community.form.type;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.ezorm.core.ValueCodec;
import org.hswebframework.ezorm.rdb.codec.*;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.ValueObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.JDBCType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.BiFunction;

@Getter
public enum BasicFieldTypes implements FieldTypeProvider, EnumDict<String> {
    String("String",
           "字符串",
           255,
           String.class,
           StringType.GLOBAL,
           JDBCType.VARCHAR,
           Arrays.asList(JDBCType.VARCHAR,
                         JDBCType.LONGVARCHAR),
           (jdbcType, conf) -> ClobValueCodec.INSTANCE),

    Byte("Byte",
         "字节",
         16,
         Byte.class,
         new IntType().max(java.lang.Byte.MAX_VALUE),
         JDBCType.SMALLINT,
         Arrays.asList(JDBCType.SMALLINT,
                       JDBCType.TINYINT),
         (jdbcType, conf) -> new NumberValueCodec(Byte.class)),

    Integer("Integer",
            "整型",
        32,
        Integer.class,
        IntType.GLOBAL,
        JDBCType.INTEGER,
        Arrays.asList(JDBCType.INTEGER,
                      JDBCType.BIGINT,
                      JDBCType.DECIMAL),
        (jdbcType, conf) -> new NumberValueCodec(Integer.class)),

    Long("Long",
         "长整型",
         64,
         Long.class,
         LongType.GLOBAL,
         JDBCType.BIGINT,
         Arrays.asList(JDBCType.BIGINT,
                       JDBCType.DECIMAL),
         (jdbcType, conf) -> new NumberValueCodec(Long.class)),

    Float(
        "Float",
        "单精度浮点型",
        32,
        Float.class,
        FloatType.GLOBAL,
        JDBCType.FLOAT,
        Arrays.asList(JDBCType.FLOAT, JDBCType.DECIMAL),
        (jdbcType, conf) -> new NumberValueCodec(Float.class)),

    Double(
        "Double",
        "双精度浮点型",
        64,
        Double.class,
        DoubleType.GLOBAL,
        JDBCType.DOUBLE,
        Arrays.asList(JDBCType.DOUBLE, JDBCType.DECIMAL),
        (jdbcType, conf) -> new NumberValueCodec(Double.class)),

    Boolean("Boolean",
            "布尔型",
            8,
            Boolean.class,
            BooleanType.GLOBAL,
            JDBCType.TINYINT,
            Arrays.asList(JDBCType.TINYINT,
                          JDBCType.BOOLEAN),
            (jdbcType, conf) -> new BooleanValueCodec(jdbcType)),

    BigDecimal("BigDecimal",
               "大浮点数",
               128,
               BigDecimal.class,
               StringType.GLOBAL,
               JDBCType.DECIMAL,
               Arrays.asList(JDBCType.DECIMAL, JDBCType.VARCHAR),
               (jdbcType, conf) -> new NumberValueCodec(BigDecimal.class)),

    BigInteger("BigInteger",
               "大整型",
               128,
               BigInteger.class,
               StringType.GLOBAL,
               JDBCType.BIGINT,
               Arrays.asList(JDBCType.BIGINT, JDBCType.VARCHAR),
               (jdbcType, conf) -> new NumberValueCodec(BigInteger.class)),

    DateTime("DateTime",
             "时间类型",
             64,
             LocalDateTime.class,
             DateTimeType.GLOBAL,
             JDBCType.TIMESTAMP,
             Arrays.asList(JDBCType.TIMESTAMP, JDBCType.DATE, JDBCType.TIME),
             (jdbcType, conf) -> {
                 Class<?> toType = LocalDateTime.class;
                 if (JDBCType.TIME.equals(jdbcType)) {
                     toType = LocalTime.class;
                 } else if (JDBCType.DATE.equals(jdbcType)) {
                     toType = LocalDate.class;
                 }
                 return new DateTimeCodec(
                     conf.getString("format", "yyyy-MM-dd HH:mm:ss"),
                     toType
                 );
             }),

    Map("Map",
        "结构体",
        255,
        String.class,
        new ObjectType(),
        JDBCType.LONGVARCHAR,
        Arrays.asList(JDBCType.LONGVARCHAR, JDBCType.VARCHAR),
        (jdbcType, conf) -> {
            BasicFieldTypes keyJavaType = conf
                .get("keyJavaType")
                .map(type -> BasicFieldTypes.valueOf(java.lang.String.valueOf(type)))
                .orElse(BasicFieldTypes.String);

            Class<?> valueJavaType = Object.class;
            java.lang.String valueJavaTypeString = conf.getString("valueJavaType", StringUtils.EMPTY);
            if (StringUtils.isNotBlank(valueJavaTypeString)) {
                valueJavaType = BasicFieldTypes.valueOf(valueJavaTypeString).getTargetJavaType();
            }
            return JsonValueCodec.ofMap(LinkedHashMap.class, keyJavaType.getTargetJavaType(), valueJavaType);
        }),

    List("List",
         "数组",
         255,
         String.class,
         new ArrayType(),
         JDBCType.LONGVARCHAR,
         Arrays.asList(JDBCType.LONGVARCHAR, JDBCType.VARCHAR),
         (jdbcType, conf) -> {
             Class<?> valueJavaType = Object.class;
             java.lang.String valueJavaTypeString = conf.getString("valueJavaType", StringUtils.EMPTY);
             if (StringUtils.isNotBlank(valueJavaTypeString)) {
                 valueJavaType = BasicFieldTypes.valueOf(valueJavaTypeString).getTargetJavaType();
             }
             return JsonValueCodec.ofCollection(ArrayList.class, valueJavaType);
         });

    private final String provider;
    private final String name;
    private final int defaultLength;
    private final Class<?> javaType;
    private final DataType datatype;
    private final JDBCType defaultJdbcType;
    private final Set<JDBCType> supportJdbcTypes;
    private final BiFunction<JDBCType, ValueObject, ValueCodec<?, ?>> codecBuilder;

    BasicFieldTypes(String provider,
                    String name,
                    int defaultLength,
                    Class<?> javaType,
                    DataType datatype,
                    JDBCType defaultJdbcType,
                    Collection<JDBCType> supportJdbcTypes,
                    BiFunction<JDBCType, ValueObject, ValueCodec<?, ?>> codecBuilder) {
        this.provider = provider;
        this.name = name;
        this.defaultLength = defaultLength;
        this.javaType = javaType;
        this.datatype = datatype;
        this.defaultJdbcType = defaultJdbcType;
        this.supportJdbcTypes = new LinkedHashSet<>(supportJdbcTypes);
        this.codecBuilder = codecBuilder;
    }


    public Class<?> getTargetJavaType() {
        if (this.equals(Map)) {
            return java.util.Map.class;
        } else if (this.equals(List)) {
            return java.util.List.class;
        }
        return this.javaType;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getProviderName() {
        return name;
    }

    @Override
    public Set<JDBCType> getSupportJdbcTypes() {
        return supportJdbcTypes;
    }

    @Override
    public FieldType create(FieldTypeSpec configuration) {
        JDBCType jdbcType = configuration.getJdbcType() == null
            ? defaultJdbcType : configuration.getJdbcType();
        if (!supportJdbcTypes.contains(jdbcType)) {
            throw new UnsupportedOperationException("unsupported jdbcType:" + jdbcType);
        }
        return SimpleFieldType.of(
            provider,
            javaType,
            jdbcType,
            codecBuilder.apply(jdbcType, configuration),
            configuration.getLength() <= 0 ? defaultLength : configuration.getLength(),
            configuration.getScale(),
            datatype
        );
    }

    public FieldType createDefault() {
        return createDefault(getDefaultLength(), 0);
    }

    public FieldType createDefault(int length) {
        return createDefault(length <= 0 ? defaultLength : length, 0);
    }


    public FieldType createDefault(int length, int scale) {
        return SimpleFieldType.of(
            provider,
            javaType,
            defaultJdbcType,
            codecBuilder.apply(defaultJdbcType, ValueObject.of(Collections.emptyMap())),
            length,
            scale,
            datatype
        );
    }


    public static void load() {
        //execute static block
    }

    static {
        for (BasicFieldTypes value : values()) {
            FieldTypeProvider.supports.register(value.getProvider(), value);
        }
        FieldTypeProvider.supports.register(EnumFieldType.TYPE, new EnumFieldType.Provider());
    }

    @Override
    public String getValue() {
        return provider;
    }

    @Override
    public java.lang.String getText() {
        return provider;
    }
}
