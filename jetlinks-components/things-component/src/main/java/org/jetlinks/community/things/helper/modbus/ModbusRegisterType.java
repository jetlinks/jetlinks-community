package org.jetlinks.community.things.helper.modbus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

import java.util.Arrays;
import java.util.Optional;

/**
 * Modbus 寄存器类型, 用于简化配置并自动推断读写功能码。
 */
@AllArgsConstructor
@Getter
public enum ModbusRegisterType implements I18nEnumDict<String> {
    /**
     * 线圈寄存器 (可读写, 1 bit)
     * 读: 0x01, 写单个: 0x05, 写多个: 0x0F
     */
    Coils("线圈寄存器", 0x01, 0x05, 0x0F, null),

    /**
     * 离散输入寄存器 (只读, 1 bit)
     * 读: 0x02
     */
    DiscreteInputs("离散输入寄存器", 0x02, null, null, null),

    /**
     * 保存寄存器 (可读写, 16 bits)
     * 读: 0x03, 写单个: 0x06, 写多个: 0x10, 掩码写: 0x16
     */
    HoldingRegisters("保存寄存器", 0x03, 0x06, 0x10, 0x16),

    /**
     * 输入寄存器 (只读, 16 bits)
     * 读: 0x04
     */
    InputRegisters("输入寄存器", 0x04, null, null, null);

    private final String text;
    private final int readFunction;
    private final Integer writeSingleFunction;
    private final Integer writeMultipleFunction;
    /**
     * 掩码写寄存器 (仅保持寄存器), 功能码 0x16；与按位写且 {@link ModbusPropertyMapping#useMaskWrite} 配合使用。
     */
    private final Integer maskWriteFunction;

    @Override
    public String getValue() {
        return name();
    }

    public boolean isReadOnly() {
        return writeSingleFunction == null;
    }

    public static Optional<ModbusRegisterType> of(String text) {
        return Arrays.stream(values())
            .filter(value -> value.name().equalsIgnoreCase(text) || value.getText().equals(text))
            .findAny();
    }

    /**
     * 根据功能码获取对应的寄存器类型
     *
     * @param functionCode 功能码
     * @return 寄存器类型，如果功能码无效则返回 null
     */
    public static ModbusRegisterType fromFunctionCode(int functionCode) {
        return Arrays.stream(values())
            .filter(type -> type.readFunction == functionCode
                    || (type.writeSingleFunction != null && type.writeSingleFunction == functionCode)
                    || (type.writeMultipleFunction != null && type.writeMultipleFunction == functionCode)
                    || (type.maskWriteFunction != null && type.maskWriteFunction == functionCode))
            .findFirst()
            .orElse(null);
    }

    /**
     * 判断该寄存器类型是否可写
     *
     * @return true if writable
     */
    public boolean isWritable() {
        return writeSingleFunction != null;
    }

    /**
     * 判断该寄存器类型是否可读
     *
     * @return true if readable
     */
    public boolean isReadable() {
        return readFunction > 0;
    }

    /**
     * 获取写功能码（优先返回写多个的功能码）
     *
     * @param writeMultiple 是否写多个
     * @return 写功能码，如果不可写则返回 -1
     */
    public int getWriteFunctionCode(boolean writeMultiple) {
        if (!isWritable()) {
            return -1;
        }
        return writeMultiple ? writeMultipleFunction : writeSingleFunction;
    }

    /**
     * 获取读功能码
     *
     * @return 读功能码
     */
    public int getReadFunctionCode() {
        return readFunction;
    }

}
