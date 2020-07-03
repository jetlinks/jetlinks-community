package org.jetlinks.community.device.web.excel;

import org.hswebframework.reactor.excel.Cell;
import org.hswebframework.reactor.excel.converter.RowWrapper;
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
    static Map<String, String> headerMapping = DeviceExcelInfo.getImportHeaderMapping();

    public static DeviceWrapper empty = new DeviceWrapper(Collections.emptyList());

    public DeviceWrapper(List<PropertyMetadata> tags) {
        for (PropertyMetadata tag : tags) {
            tagMapping.put(tag.getName(), tag);
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
        if (maybeTag != null) {
            deviceExcelInfo.tag(maybeTag.getId(), headerText, cell.value().orElse(null), maybeTag.getValueType().getId());
        } else {
            deviceExcelInfo.with(headerMapping.getOrDefault(headerText, headerText), cell.value().orElse(null));
        }
        deviceExcelInfo.setRowNumber(cell.getRowIndex());

        return deviceExcelInfo;
    }
}
