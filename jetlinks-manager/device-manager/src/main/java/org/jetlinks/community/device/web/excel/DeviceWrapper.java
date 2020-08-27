package org.jetlinks.community.device.web.excel;

import org.hswebframework.reactor.excel.Cell;
import org.hswebframework.reactor.excel.converter.RowWrapper;
import org.jetlinks.core.metadata.ConfigPropertyMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备数据导入包装器
 *
 * @author zhouhao
 * @see 1.0
 */
public class DeviceWrapper extends RowWrapper<DeviceExcelInfo> {

    Map<String, PropertyMetadata> tagMapping = new HashMap<>();

    Map<String, ConfigPropertyMetadata> configMapping = new HashMap<>();

    static Map<String, String> headerMapping = DeviceExcelInfo.getImportHeaderMapping();

    public static DeviceWrapper empty = new DeviceWrapper(Collections.emptyList(), Collections.emptyList());

    public DeviceWrapper(List<PropertyMetadata> tags, List<ConfigPropertyMetadata> configs) {
        for (PropertyMetadata tag : tags) {
            tagMapping.put(tag.getName(), tag);
        }
        for (ConfigPropertyMetadata config : configs) {
            configMapping.put(config.getName(), config);
        }
    }

    @Override
    protected DeviceExcelInfo newInstance() {
        return new DeviceExcelInfo();
    }

    @Override
    protected DeviceExcelInfo wrap(DeviceExcelInfo deviceExcelInfo, Cell header, Cell cell) {
        String headerText = header.valueAsText().orElse("null");

        PropertyMetadata maybeTag = tagMapping.get(headerText);
        ConfigPropertyMetadata maybeConfig = configMapping.get(headerText);
        if (maybeTag != null) {
            deviceExcelInfo.tag(maybeTag.getId(), headerText, cell.value().orElse(null), maybeTag.getValueType().getId());
        } else if (maybeConfig != null) {
            deviceExcelInfo.config(maybeConfig.getProperty(), cell.value().orElse(null));
        } else {
            deviceExcelInfo.with(headerMapping.getOrDefault(headerText, headerText), cell.value().orElse(null));
        }
        deviceExcelInfo.setRowNumber(cell.getRowIndex());

        return deviceExcelInfo;
    }
}
