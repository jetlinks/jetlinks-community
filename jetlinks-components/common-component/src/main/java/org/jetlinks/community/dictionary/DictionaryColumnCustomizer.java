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
