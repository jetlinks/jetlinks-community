package org.jetlinks.community.things.helper.modbus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 抽象的 Modbus 写请求描述, 默认按寄存器为单位。
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModbusWriteRequest {

    /**
     * 从站ID
     */
    private int slaveId;

    /**
     * 功能码
     */
    private int functionCode;

    /**
     * 起始寄存器地址
     */
    private int address;

    /**
     * 写入寄存器数量
     */
    private int quantity;

    /**
     * 要写入的寄存器值, 单位: 寄存器(每个元素2字节)。
     */
    private int[] values;

}

