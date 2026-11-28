package org.jetlinks.community.things.helper.modbus;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Modbus 寄存器解码器，负责将寄存器快照解析为物模型属性值。
 * <p>
 * 解析流程：
 * <ol>
 *   <li>根据属性映射定位寄存器值</li>
 *   <li>使用寄存器定义中的 Codec 解析数值</li>
 *   <li>应用按位提取或元素索引提取</li>
 *   <li>返回属性ID到属性值的映射</li>
 * </ol>
 *
 * @author zhou
 */
public class ModbusRegistersDecoder {

    /**
     * 解码寄存器快照为物模型属性值
     *
     * @param snapshot 寄存器快照
     * @param mapping  映射配置
     * @return 属性ID到属性值的映射
     */
    public static Map<String, Object> decode(ModbusRegisterSnapshot snapshot,
                                              ModbusThingsMapping mapping) {
        if (snapshot == null || mapping == null) {
            return new HashMap<>();
        }

        Map<String, Object> properties = new HashMap<>();
        Map<ModbusRegisterKey, Integer> registers = snapshot.getValues();

        if (registers == null || registers.isEmpty()) {
            return properties;
        }

        // 先按寄存器维度解码，避免重复解码同一寄存器
        Map<ModbusRegisterKey, Object> decodedRegisters = new HashMap<>();

        for (ModbusPropertyMapping propertyMapping : mapping.getProperties()) {
            if (propertyMapping == null
                || !propertyMapping.isReadable()
                || propertyMapping.getProperty() == null
                || propertyMapping.getRegister() == null) {
                continue;
            }

            ModbusRegisterKey key = propertyMapping.getRegister().getKey();
            if (key == null || !registers.containsKey(key)) {
                continue;
            }

            // 获取或解码寄存器值
            Object decodedValue = decodedRegisters.computeIfAbsent(key, k -> {
                Integer registerValue = registers.get(k);
                return decodeRegister(registerValue, propertyMapping.getRegister());
            });

            ModbusRegisterDefinition def = propertyMapping.getRegister();
            Object propertyValue = propertyMapping.isBitOperation()
                ? extractPropertyValue(decodedValue, propertyMapping)
                : extractPropertyValue(def.applyScaleAfterDecode(decodedValue), propertyMapping);
            if (propertyValue != null) {
                properties.put(propertyMapping.getProperty(), propertyValue);
            }
        }

        return properties;
    }

    /**
     * 使用寄存器定义解码单个寄存器值
     *
     * @param registerValue 寄存器原始值（16位无符号整数）
     * @param definition    寄存器定义
     * @return 解码后的值
     */
    private static Object decodeRegister(Integer registerValue, ModbusRegisterDefinition definition) {
        if (registerValue == null || definition == null) {
            return null;
        }
        ByteBuf buf = Unpooled.buffer(2);
        buf.writeShort(registerValue & 0xFFFF);
        try {
            return definition.decode(buf);
        } catch (Exception e) {
            return decodeWithScale(registerValue);
        } finally {
            buf.release();
        }
    }

    /**
     * definition.decode 失败时的回退：仅返回无符号 16 位原始值（缩放由 {@link ModbusRegisterDefinition#applyScaleAfterDecode} 统一处理）。
     */
    private static Object decodeWithScale(Integer registerValue) {
        return registerValue & 0xFFFF;
    }

    /**
     * 从解码结果中提取属性值
     *
     * @param decodedValue      解码后的寄存器值
     * @param propertyMapping   属性映射
     * @return 最终的属性值
     */
    private static Object extractPropertyValue(Object decodedValue, ModbusPropertyMapping propertyMapping) {
        if (decodedValue == null) {
            return null;
        }

        // 优先处理按位提取
        if (propertyMapping.isBitOperation()) {
            return extractBits(decodedValue, propertyMapping);
        }

        // 处理元素索引提取
        if (propertyMapping.getElementIndex() != null && propertyMapping.getElementIndex() >= 0) {
            return extractElement(decodedValue, propertyMapping.getElementIndex());
        }

        // 直接返回解码结果
        return decodedValue;
    }

    /**
     * 按位提取属性值
     *
     * @param value           原始值
     * @param propertyMapping 属性映射
     * @return 提取后的位值
     */
    private static Object extractBits(Object value, ModbusPropertyMapping propertyMapping) {
        int bitIndex = propertyMapping.getBitIndex();
        int bitLength = propertyMapping.getBitLengthSafe();

        int intValue;
        if (value instanceof Number) {
            intValue = ((Number) value).intValue();
        } else if (value instanceof Boolean) {
            intValue = ((Boolean) value) ? 1 : 0;
        } else {
            try {
                intValue = new BigDecimal(String.valueOf(value)).intValue();
            } catch (Exception e) {
                return null;
            }
        }

        // 提取指定位
        int mask = ((1 << bitLength) - 1) << bitIndex;
        int bits = (intValue & mask) >>> bitIndex;

        // 如果是1位，返回布尔值；否则返回数值
        return bitLength == 1 ? (bits == 1) : bits;
    }

    /**
     * 从数组或集合中提取指定下标的元素
     *
     * @param decodedValue 解码后的值
     * @param index        元素下标
     * @return 提取的元素
     */
    @SuppressWarnings("all")
    private static Object extractElement(Object decodedValue, int index) {
        if (decodedValue == null || index < 0) {
            return null;
        }

        if (decodedValue.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(decodedValue);
            if (index >= length) {
                return null;
            }
            return java.lang.reflect.Array.get(decodedValue, index);
        }

        if (decodedValue instanceof Iterable) {
            int i = 0;
            for (Object item : (Iterable) decodedValue) {
                if (i == index) {
                    return item;
                }
                i++;
            }
            return null;
        }

        // 不是数组或集合，直接返回原值
        return decodedValue;
    }

    /**
     * 响应式版本的解码方法
     *
     * @param snapshot 寄存器快照
     * @param mapping  映射配置
     * @return 属性ID到属性值的映射
     */
    public static Mono<Map<String, Object>> decodeReactive(ModbusRegisterSnapshot snapshot,
                                                            ModbusThingsMapping mapping) {
        return Mono.fromSupplier(() -> decode(snapshot, mapping));
    }

}
