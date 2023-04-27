package org.jetlinks.community.device.web.excel;

import org.hswebframework.reactor.excel.Cell;
import org.hswebframework.reactor.excel.converter.RowWrapper;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.ConfigPropertyMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyMetadataWrapper extends RowWrapper<PropertyMetadataExcelInfo> {

    private final Map<String, String> propertyMapping = new HashMap<>();

    public PropertyMetadataWrapper(List<ConfigMetadata> expands) {
        propertyMapping.put("属性ID", "property");
        propertyMapping.put("属性名称", "name");
        propertyMapping.put("数据类型", "dataType");
        propertyMapping.put("单位", "unit");
        propertyMapping.put("精度", "scale");
        propertyMapping.put("数据类型配置", "valueType");
        propertyMapping.put("来源", "source");
        propertyMapping.put("属性说明", "description");
        propertyMapping.put("读写类型", "type");
        for (ConfigMetadata expand : expands) {
            for (ConfigPropertyMetadata property : expand.getProperties()) {
                propertyMapping.put(expand.getName() + "-" + property.getName(), property.getProperty());
                propertyMapping.put(property.getName(), property.getProperty());
            }
        }
    }

    @Override
    protected PropertyMetadataExcelInfo newInstance() {
        return new PropertyMetadataExcelInfo();
    }

    @Override
    protected PropertyMetadataExcelInfo wrap(PropertyMetadataExcelInfo instance, Cell header, Cell dataCell) {
        String headerText = header.valueAsText().orElse("null");
        Object value = dataCell.valueAsText().orElse("");
        if (propertyMapping.containsKey(headerText)) {
            instance.with(propertyMapping.get(headerText), propertyTypeToLowerCase(headerText, value));
        }
        instance.setRowNumber(dataCell.getRowIndex() + 1);
        return instance;
    }

    private Object propertyTypeToLowerCase(String headerText, Object value) {
        if ("类型".equals(headerText)) {
            return value.toString().toLowerCase();
        }
        return value;
    }
}
