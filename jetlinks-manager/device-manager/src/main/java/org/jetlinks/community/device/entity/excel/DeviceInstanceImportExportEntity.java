package org.jetlinks.community.device.entity.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class DeviceInstanceImportExportEntity {

    @ExcelProperty("设备ID")
    private String id;

    @ExcelProperty("设备名称")
    private String name;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("描述")
    private String describe;

    @ExcelProperty("父级设备ID")
    private String parentId;


}
