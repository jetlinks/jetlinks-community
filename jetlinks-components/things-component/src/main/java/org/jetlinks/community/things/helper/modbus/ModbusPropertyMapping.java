package org.jetlinks.community.things.helper.modbus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 描述物模型属性与 Modbus 寄存器之间的映射关系。
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModbusPropertyMapping {

    /**
     * 物模型属性标识
     */
    private String property;

    /**
     * 所属寄存器定义
     */
    private ModbusRegisterDefinition register;

    /**
     * 当 {@link ModbusRegisterDefinition} 中配置的 Codec 解码结果为数组或集合时,
     * 通过该下标(从0开始)提取对应元素。
     * <p>
     * 未配置时, 默认使用 Codec 解码后的完整结果。
     */
    private Integer elementIndex;

    /**
     * 位偏移(从0开始), 用于按位解析.
     * <p>
     * 当未配置 {@link #elementIndex} 且未配置 {@link ModbusRegisterDefinition#codec} 时,
     * 则直接按寄存器 16 位值的对应位提取.
     */
    private Integer bitIndex;

    /**
     * 位长度, 默认为 1.
     */
    private Integer bitLength;

    /**
     * 保持寄存器按位写时是否使用功能码 0x16（Mask Write Register）。
     * <p>
     * 默认关闭：仍合并为整字后走 0x06/0x10。开启且映射为保持寄存器按位写时，下行生成掩码写 PDU（需设备支持 FC22）。
     */
    private Boolean useMaskWrite;

    /**
     * 读写方向: 是否可读
     */
    private boolean readable = true;

    /**
     * 读写方向: 是否可写
     */
    private boolean writable = true;

    /**
     * 可选描述信息
     */
    private String description;

    /**
     * 判断是否配置了按位解析
     *
     * @return true if bit parsing is configured
     */
    public boolean isBitOperation() {
        return bitIndex != null && bitIndex >= 0;
    }

    /**
     * 获取位长度，默认为 1
     *
     * @return bit length
     */
    public int getBitLengthSafe() {
        return bitLength != null && bitLength > 0 ? bitLength : 1;
    }

    /**
     * 获取位掩码，用于按位提取和修改
     *
     * @return bit mask
     */
    public int getBitMask() {
        int length = getBitLengthSafe();
        return ((1 << length) - 1) << (bitIndex != null ? bitIndex : 0);
    }

}

