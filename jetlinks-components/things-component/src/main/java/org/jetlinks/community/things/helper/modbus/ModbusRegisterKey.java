package org.jetlinks.community.things.helper.modbus;

import lombok.*;

/**
 * 表示一个 Modbus 寄存器键，用于唯一标识一个寄存器。
 * <p>
 * 字符串表示格式: {@code 从站ID_类型_地址}，例如 {@code 1_HoldingRegisters_0}
 *
 * @author zhou
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ModbusRegisterKey {

    /**
     * 从站ID
     */
    private int slaveId;

    /**
     * 寄存器类型
     */
    private ModbusRegisterType type;

    /**
     * 寄存器地址
     */
    private int address;

    /**
     * 根据功能码创建寄存器键
     *
     * @param slaveId      从站ID
     * @param functionCode 功能码
     * @param address      寄存器地址
     * @return 寄存器键
     * @throws IllegalArgumentException 如果功能码无效
     */
    public static ModbusRegisterKey of(int slaveId, int functionCode, int address) {
        ModbusRegisterType registerType = ModbusRegisterType.fromFunctionCode(functionCode);
        if (registerType == null) {
            throw new IllegalArgumentException("Invalid function code: " + functionCode);
        }
        return new ModbusRegisterKey(slaveId, registerType, address);
    }

    /**
     * 获取读功能码
     *
     * @return 读功能码
     */
    public int getReadFunctionCode() {
        return type != null ? type.getReadFunction() : -1;
    }

    /**
     * 获取写功能码
     *
     * @param writeMultiple 是否写多个
     * @return 写功能码，如果不可写则返回 -1
     */
    public int getWriteFunctionCode(boolean writeMultiple) {
        if (type == null || !type.isWritable()) {
            return -1;
        }
        return writeMultiple ? type.getWriteMultipleFunction() : type.getWriteSingleFunction();
    }

    @Override
    public String toString() {
        return slaveId + "_" + (type != null ? type.name() : "UNKNOWN") + "_" + address;
    }

}

