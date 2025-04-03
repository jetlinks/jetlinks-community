package org.jetlinks.community.things.data.operations;

import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.core.things.ThingMetadata;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public abstract class RowModeDDLOperationsBase extends AbstractDDLOperations{

    public RowModeDDLOperationsBase(String thingType,
                                    String templateId,
                                    String thingId,
                                    DataSettings settings,
                                    MetricBuilder metricBuilder) {
        super(thingType, templateId, thingId, settings, metricBuilder);
    }

    @Override
    protected List<PropertyMetadata> createPropertyProperties(List<PropertyMetadata> propertyMetadata) {
        List<PropertyMetadata> props = new ArrayList<>(createBasicColumns());

        this.validateMetadata(propertyMetadata,props);

        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_ID, "属性ID", StringType.GLOBAL));

        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_NUMBER_VALUE, "数字值", DoubleType.GLOBAL));
        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_GEO_VALUE, "地理位置值", GeoType.GLOBAL));

        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_ARRAY_VALUE, "数组值", new ArrayType()));
        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_OBJECT_VALUE, "对象值", new ObjectType()));
        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_TIME_VALUE, "时间值", DateTimeType.GLOBAL));

        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_VALUE, "原始值", StringType.GLOBAL));


        return props;
    }

    //是否只支持一个对象或数组类型的属性
    protected boolean isOnlySupportsOneObjectOrArrayProperty() {
        return false;
    }

    @Override
    public Mono<Void> validateMetadata(ThingMetadata metadata) {
        this.validateMetadata(metadata.getProperties(), null);
        return Mono.empty();
    }

    private void validateMetadata(List<PropertyMetadata> propertyMetadata, List<PropertyMetadata> props){
        ArrayType arrayType = null;
        ObjectType objectType = null;
        if (isOnlySupportsOneObjectOrArrayProperty()) {
            for (PropertyMetadata metadata : propertyMetadata) {
                if (ThingsDataConstants.propertyIsJsonStringStorage(metadata)) {
                    continue;
                }
                if (metadata.getValueType() instanceof ArrayType) {
                    if (arrayType != null) {
                        throw new I18nSupportException("error.thing_storage_only_supports_one_array_type", metadata.getId());
                    }
                    arrayType = (ArrayType) metadata.getValueType();
                }
                if (metadata.getValueType() instanceof ObjectType) {
                    if (objectType != null) {
                        throw new I18nSupportException("error.thing_storage_only_supports_one_object_type", metadata.getId());
                    }
                    objectType = (ObjectType) metadata.getValueType();
                }
            }
        }
        if (props == null) {
            return;
        }
        if (arrayType == null) {
            arrayType = new ArrayType();
        }
        if (objectType == null) {
            objectType = new ObjectType();
        }
        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_ARRAY_VALUE, "数组值", arrayType));
        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_OBJECT_VALUE, "对象值", objectType));
    }
}
