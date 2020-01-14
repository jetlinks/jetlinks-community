package org.jetlinks.community.device.events.handler;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.elastic.search.enums.FieldDateFormat;
import org.jetlinks.community.elastic.search.enums.FieldType;
import org.jetlinks.community.elastic.search.index.mapping.MappingFactory;

import java.util.List;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class ValueTypeRecordToESHandler {


    public static MappingFactory handler(MappingFactory mappingFactory, DataType dataType, String name) {
        if (dataType instanceof DateTimeType) {
            return mappingFactory
                    .addFieldName(name)
                    .addFieldType(FieldType.DATE)
                    .addFieldDateFormat(FieldDateFormat.epoch_millis, FieldDateFormat.simple_date, FieldDateFormat.strict_date_time)
                    .commit();
        } else if (dataType instanceof StringType) {
            return packageCreateIndexRequest(mappingFactory, name, FieldType.KEYWORD);
        } else if (dataType instanceof DoubleType) {
            return packageCreateIndexRequest(mappingFactory, name, FieldType.DOUBLE);
        } else if (dataType instanceof FloatType) {
            return packageCreateIndexRequest(mappingFactory, name, FieldType.FLOAT);
        } else if (dataType instanceof LongType) {
            return packageCreateIndexRequest(mappingFactory, name, FieldType.LONG);
        } else if (dataType instanceof BooleanType) {
            return packageCreateIndexRequest(mappingFactory, name, FieldType.BOOLEAN);
        } else if (dataType instanceof IntType) {
            return packageCreateIndexRequest(mappingFactory, name, FieldType.INTEGER);
        } else if (dataType instanceof ObjectType) {
            return propertyMetadataTranslator(mappingFactory, ((ObjectType) dataType).getProperties());
        } else {
            return packageCreateIndexRequest(mappingFactory, name, FieldType.KEYWORD);
        }
    }

    private static MappingFactory propertyMetadataTranslator(MappingFactory mappingFactory, List<PropertyMetadata> metadataList) {
        metadataList.forEach(propertyMetadata -> handler(mappingFactory, propertyMetadata.getValueType(), propertyMetadata.getId()));
        return mappingFactory;
    }

    private static MappingFactory packageCreateIndexRequest(MappingFactory mappingFactory, String name, FieldType type) {
        return mappingFactory
                .addFieldName(name)
                .addFieldType(type)
                .commit();
    }
}
