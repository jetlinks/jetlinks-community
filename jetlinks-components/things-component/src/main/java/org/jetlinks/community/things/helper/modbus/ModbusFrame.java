package org.jetlinks.community.things.helper.modbus;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 简单的 Modbus 帧抽象, 用于在 ByteBuf 与寄存器读写请求之间做承载。
 * <p>
 * 这里仅关注逻辑字段, 不关心底层是 RTU 还是 TCP, 也不处理 CRC 等链路相关细节。
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModbusFrame {

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
     * 寄存器数据区。
     * <p>
     * 对于读响应, 表示读取到的寄存器原始字节;
     * 对于写请求, 表示要写入的寄存器原始字节.
     */
    private ByteBuf values;

}

