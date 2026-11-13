package org.jetlinks.community.things.helper.modbus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 抽象的 Modbus 读请求描述, 不关心底层报文格式。
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModbusReadRequest {

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
     * 读取寄存器数量
     */
    private int quantity;

}

