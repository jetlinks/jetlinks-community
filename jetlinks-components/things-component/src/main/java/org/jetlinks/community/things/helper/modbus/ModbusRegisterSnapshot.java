package org.jetlinks.community.things.helper.modbus;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于在转换过程中保存寄存器的当前值快照。
 * <p>
 * 这里使用 int 表示寄存器值(0-65535)，具体有符号处理在映射阶段完成。
 */
@Getter
@AllArgsConstructor
public class ModbusRegisterSnapshot {

    private final Map<ModbusRegisterKey, Integer> values;

    public static ModbusRegisterSnapshot of(Map<ModbusRegisterKey, Integer> values) {
        if (values == null || values.isEmpty()) {
            return new ModbusRegisterSnapshot(Collections.emptyMap());
        }
        return new ModbusRegisterSnapshot(Collections.unmodifiableMap(new HashMap<>(values)));
    }

    public Integer get(ModbusRegisterKey key) {
        return values.get(key);
    }

}

