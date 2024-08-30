package org.jetlinks.community.form.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hswebframework.ezorm.core.ValueCodec;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.community.dictionary.Dictionaries;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;

import java.sql.JDBCType;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @since 2.1
 */
@Getter
@RequiredArgsConstructor
public class EnumFieldType implements FieldType, ValueCodec<Object, Object> {
    public static final String TYPE = "Enum";

    private final boolean multiple;

    private final String dictId;

    private final JDBCType jdbcType;

    private boolean array = false;

    private Function<EnumDict<?>,Object> fieldValueConverter = EnumDict::getWriteJSONObject;;

    public EnumFieldType withArray(boolean array) {
        this.array = array;
        return this;
    }

    public EnumFieldType withFieldValueConverter(Function<EnumDict<?>,Object> converter) {
        this.fieldValueConverter = converter;
        return this;
    }


    @Override
    public final String getId() {
        return TYPE;
    }

    public boolean isToMask() {
        return multiple && jdbcType == JDBCType.BIGINT;
    }

    @Override
    public Class<?> getJavaType() {
        return isToMask() ? Long.class : String.class;
    }

    @Override
    public JDBCType getJdbcType() {
        return jdbcType;
    }

    @Override
    public int getLength() {
        return 64;
    }

    @Override
    public DataType getDataType() {
        EnumType enumType = new EnumType();
        for (EnumDict<?> item : Dictionaries.getItems(dictId)) {
            enumType.addElement(EnumType.Element.of(String.valueOf(item.getValue()), item.getText()));
        }
        enumType.setMulti(multiple);
        return enumType;
    }

    @Override
    public ValueCodec<?, ?> getCodec() {
        return this;
    }

    @Override
    public Object encode(Object value) {
        //转为位掩码
        if (isToMask()) {
            return Dictionaries.toMask(
                ConverterUtils
                    .convertToList(value, val -> Dictionaries.findItem(dictId, val).orElse(null))
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        }
        //多选,使用逗号分隔
        if (multiple) {
            return ConverterUtils
                .convertToList(value, val -> Dictionaries.findItem(dictId, val).orElse(null))
                .stream()
                .filter(Objects::nonNull)
                .map(e -> String.valueOf(e.getValue()))
                .collect(Collectors.joining(","));
        }
        return Dictionaries
                .findItem(dictId, value)
                .<Object>map(EnumDict::getValue)
                .orElseGet(() -> {
                    if (value instanceof EnumDict) {
                        return ((EnumDict<?>) value).getValue();
                    }
                    return value;
                });
    }

    @Override
    public Object decode(Object data) {
        if (multiple) {
            List<Object> list;
            if (isToMask()) {
                list =
                    Dictionaries
                        .getItems(dictId, CastUtils.castNumber(data).longValue())
                        .stream()
                        .map(fieldValueConverter)
                        .collect(Collectors.toList());
            } else {
                list = ConverterUtils
                    .convertToList(data, val -> Dictionaries.findItem(dictId, val).orElse(null))
                    .stream()
                    .filter(Objects::nonNull)
                    .map(fieldValueConverter)
                    .collect(Collectors.toList());
            }
            if (isArray()) {
                return list.toArray(new EnumDict[0]);
            }
            return list;
        }
       if(isToMask()){
              return Dictionaries
                .getItems(dictId, CastUtils.castNumber(data).longValue())
                .stream()
                .map(fieldValueConverter)
                .findFirst()
                .orElse(null);
       }
        return Dictionaries
            .findItem(dictId, data)
            .map(fieldValueConverter)
            .orElse(fieldValueConverter.apply(EnumDict.create(String.valueOf(data))));
    }

    public static class Provider implements FieldTypeProvider {

        @Override
        public String getProvider() {
            return EnumFieldType.TYPE;
        }

        @Override
        public String getProviderName() {
            return "枚举";
        }

        @Override
        public Set<JDBCType> getSupportJdbcTypes() {
            return new HashSet<>(Arrays.asList(JDBCType.VARCHAR, JDBCType.BIGINT));
        }

        @Override
        public int getDefaultLength() {
            return 64;
        }

        @Override
        public FieldType create(FieldTypeSpec configuration) {
            DictionarySpec spec = Optional
                .ofNullable(configuration.getConfiguration())
                .map(configSpec -> FastBeanCopier.copy(configSpec, new DictionarySpec()))
                .orElse(new DictionarySpec());
            return new EnumFieldType(
                spec.isMultiple(),
                Optional
                    .ofNullable(spec.getDictionaryId())
                    .orElseThrow(() -> new IllegalArgumentException("dictionaryId can not be null")),
                configuration.getJdbcType() == null ? JDBCType.VARCHAR : configuration.getJdbcType()
            );
        }
    }

    //数据字典
    @Getter
    @Setter
    public static class DictionarySpec {
        //字典ID
        private String dictionaryId;
        //多选
        private boolean multiple;
    }
}
