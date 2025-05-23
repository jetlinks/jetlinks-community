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

import org.hswebframework.reactor.excel.Cell;
import org.hswebframework.reactor.excel.converter.RowWrapper;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.ConfigPropertyMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyMetadataImportWrapper extends RowWrapper<PropertyMetadataExcelImportInfo> {

    private final Map<String, String> propertyMapping = new HashMap<>();
    private final Map<String, String> expandsMapping = new HashMap<>();

    public PropertyMetadataImportWrapper(List<ConfigMetadata> expands) {
        propertyMapping.put("属性标识", "property");
        propertyMapping.put("属性名称", "name");
        propertyMapping.put("数据类型", "dataType");
        propertyMapping.put("单位", "unit");
        propertyMapping.put("精度", "scale");
        propertyMapping.put("数据类型配置", "valueType");
        propertyMapping.put("来源", "source");
        propertyMapping.put("属性说明", "description");
        propertyMapping.put("读写类型", "type");
        propertyMapping.put("存储方式", "storageType");
        for (ConfigMetadata expand : expands) {
            for (ConfigPropertyMetadata property : expand.getProperties()) {
                expandsMapping.put(expand.getName() + "-" + property.getName(), property.getProperty());
                expandsMapping.put(property.getName(), property.getProperty());
            }
        }
    }

    @Override
    protected PropertyMetadataExcelImportInfo newInstance() {
        return new PropertyMetadataExcelImportInfo();
    }

    @Override
    protected PropertyMetadataExcelImportInfo wrap(PropertyMetadataExcelImportInfo instance, Cell header, Cell dataCell) {
        String headerText = header.valueAsText().orElse("null");
        Object value = dataCell.valueAsText().orElse("");
        if (propertyMapping.containsKey(headerText)) {
            instance.with(propertyMapping.get(headerText), propertyTypeToLowerCase(headerText, value));
        }
        if (expandsMapping.containsKey(headerText)) {
            instance.withExpands(expandsMapping.get(headerText), propertyTypeToLowerCase(headerText, value));
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
