package org.jetlinks.community.things.data.operations;

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.things.data.ThingsDataConstants;

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
        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_ID, "属性ID", StringType.GLOBAL));

        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_NUMBER_VALUE, "数字值", DoubleType.GLOBAL));
        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_GEO_VALUE, "地理位置值", GeoType.GLOBAL));

        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_ARRAY_VALUE, "数组值", new ArrayType()));
        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_OBJECT_VALUE, "对象值", new ObjectType()));
        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_TIME_VALUE, "时间值", DateTimeType.GLOBAL));

        props.add(SimplePropertyMetadata.of(ThingsDataConstants.COLUMN_PROPERTY_VALUE, "原始值", StringType.GLOBAL));


        return props;
    }
}
