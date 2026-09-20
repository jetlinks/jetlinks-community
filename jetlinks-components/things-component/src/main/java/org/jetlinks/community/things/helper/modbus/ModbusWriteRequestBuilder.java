package org.jetlinks.community.things.helper.modbus;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.codec.layout.ByteLayout;
import org.jetlinks.core.codec.layout.ByteLayouts;
import org.jetlinks.core.utils.ConverterUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 将物模型写入操作转换为 Modbus 写请求。
 * <p>
 * 主要职责:
 * <ul>
 *     <li>按寄存器聚合多个写操作;</li>
 *     <li>在按位写入时基于快照进行读改写, 保证未修改位保持最新值;</li>
 *     <li>在保持寄存器按位写且 {@link ModbusPropertyMapping#getUseMaskWrite()} 为 true 时生成 FC 0x16（掩码写）;</li>
 *     <li>生成按寄存器为单位的写请求描述。</li>
 * </ul>
 */
public class ModbusWriteRequestBuilder {

    /**
     * 根据物模型写入操作、映射配置和当前寄存器快照构建写请求。
     *
     * @param operations 写入操作
     * @param mapping    映射配置
     * @param snapshot   当前寄存器快照, 用于按位读改写
     * @return 写请求列表
     */
    public List<ModbusWriteRequest> build(List<ThingWriteOperation> operations,
                                          ModbusThingsMapping mapping,
                                          ModbusRegisterSnapshot snapshot) {
        if (operations == null || operations.isEmpty() || mapping == null) {
            return List.of();
        }
        Map<ModbusRegisterKey, WriteMergeState> states = new HashMap<>();
        if (snapshot != null && snapshot.getValues() != null) {
            snapshot.getValues().forEach((k, v) -> {
                WriteMergeState s = new WriteMergeState();
                s.value = v & 0xFFFF;
                states.put(k, s);
            });
        }

        for (ThingWriteOperation operation : operations) {
            if (operation == null || operation.getProperty() == null) {
                continue;
            }
            mapping
                .getProperty(operation.getProperty())
                .ifPresent(prop -> applyWriteOperation(prop, operation.getValue(), states));
        }

        if (states.isEmpty()) {
            return List.of();
        }

        List<ModbusWriteRequest> requests = new ArrayList<>();
        Map<SlaveFunctionKey, List<Map.Entry<ModbusRegisterKey, WriteMergeState>>> groups = new HashMap<>();
        for (Map.Entry<ModbusRegisterKey, WriteMergeState> e : states.entrySet()) {
            ModbusRegisterKey k = e.getKey();
            groups.computeIfAbsent(new SlaveFunctionKey(k.getSlaveId(), k.getType()), x -> new ArrayList<>())
                  .add(e);
        }

        for (Map.Entry<SlaveFunctionKey, List<Map.Entry<ModbusRegisterKey, WriteMergeState>>> groupEntry : groups.entrySet()) {
            SlaveFunctionKey groupKey = groupEntry.getKey();
            List<Map.Entry<ModbusRegisterKey, WriteMergeState>> sortedEntries = groupEntry.getValue().stream()
                .sorted(Comparator.comparingInt(en -> en.getKey().getAddress()))
                .toList();

            int i = 0;
            while (i < sortedEntries.size()) {
                Map.Entry<ModbusRegisterKey, WriteMergeState> e = sortedEntries.get(i);
                ModbusRegisterKey regKey = e.getKey();
                WriteMergeState st = e.getValue();
                ModbusRegisterType type = regKey.getType();

                if (shouldEmitMaskWrite(st, type)) {
                    int andMask = 0xFFFF & ~st.replaceMaskBits;
                    int orMask = (st.value & st.replaceMaskBits) & 0xFFFF;
                    requests.add(new ModbusWriteRequest(
                        groupKey.slaveId,
                        type.getMaskWriteFunction(),
                        regKey.getAddress(),
                        1,
                        new int[]{andMask, orMask}));
                    i++;
                } else {
                    int startAddr = regKey.getAddress();
                    List<Integer> vals = new ArrayList<>();
                    vals.add(st.value & 0xFFFF);
                    i++;
                    while (i < sortedEntries.size()) {
                        Map.Entry<ModbusRegisterKey, WriteMergeState> e2 = sortedEntries.get(i);
                        if (shouldEmitMaskWrite(e2.getValue(), e2.getKey().getType())) {
                            break;
                        }
                        if (e2.getKey().getAddress() != startAddr + vals.size()) {
                            break;
                        }
                        vals.add(e2.getValue().value & 0xFFFF);
                        i++;
                    }
                    requests.add(createRequest(groupKey, startAddr, vals));
                }
            }
        }
        return requests;
    }

    private static boolean shouldEmitMaskWrite(WriteMergeState st, ModbusRegisterType type) {
        return type == ModbusRegisterType.HoldingRegisters
            && type.getMaskWriteFunction() != null
            && st.maskBitActivity
            && !st.nonMaskActivity
            && st.replaceMaskBits != 0;
    }

    private ModbusWriteRequest createRequest(SlaveFunctionKey key, int startAddress, List<Integer> values) {
        int[] vals = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vals[i] = values.get(i);
        }
        ModbusRegisterType type = key.type;
        int functionCode = type != null ? type.getReadFunction() : 0;
        if (type != null && !type.isReadOnly()) {
            if (vals.length > 1) {
                functionCode = type.getWriteMultipleFunction();
            } else {
                functionCode = type.getWriteSingleFunction();
            }
        }
        return new ModbusWriteRequest(key.slaveId, functionCode, startAddress, vals.length, vals);
    }

    @lombok.AllArgsConstructor
    @lombok.EqualsAndHashCode
    private static class SlaveFunctionKey {
        int slaveId;
        ModbusRegisterType type;

        public int getFunctionCode() {
            return type != null ? type.getReadFunction() : 0;
        }
    }

    private static final class WriteMergeState {
        int value;
        /** 是否存在仅掩码方式的按位写（无整字/非掩码按位混用） */
        boolean maskBitActivity;
        /** 存在整字写或非掩码按位写 */
        boolean nonMaskActivity;
        int replaceMaskBits;
    }

    private void applyWriteOperation(ModbusPropertyMapping mapping,
                                     Object value,
                                     Map<ModbusRegisterKey, WriteMergeState> states) {
        ModbusRegisterDefinition def = mapping.getRegister();
        if (def == null || def.getKey() == null) {
            return;
        }
        ModbusRegisterKey key = def.getKey();
        WriteMergeState state = states.computeIfAbsent(key, k -> new WriteMergeState());
        int original = state.value & 0xFFFF;

        Integer elementIndex = mapping.getElementIndex();
        Integer bitIndex = mapping.getBitIndex();

        if (def.getCodec() != null && !def.getCodec().isEmpty() && elementIndex != null && elementIndex >= 0) {
            Object aggregate = decodeByDefinition(original, def);
            aggregate = applyElementUpdate(aggregate, elementIndex, value);
            int encoded = encodeNumericValue(aggregate, def);
            state.value = encoded & 0xFFFF;
            state.nonMaskActivity = true;
        } else if (bitIndex != null && bitIndex >= 0) {
            int bitLength = mapping.getBitLength() == null ? 1 : mapping.getBitLength();
            int mask = (1 << bitLength) - 1;
            int val = toInt(value) & mask;
            int encoded = (original & ~(mask << bitIndex)) | (val << bitIndex);
            state.value = encoded & 0xFFFF;
            boolean maskWrite = Boolean.TRUE.equals(mapping.getUseMaskWrite())
                && key.getType() == ModbusRegisterType.HoldingRegisters;
            if (maskWrite) {
                if (!state.nonMaskActivity) {
                    state.maskBitActivity = true;
                    state.replaceMaskBits |= mapping.getBitMask();
                }
            } else {
                state.nonMaskActivity = true;
            }
        } else {
            int encoded = encodeNumericValue(value, def);
            state.value = encoded & 0xFFFF;
            state.nonMaskActivity = true;
        }
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        return new BigDecimal(String.valueOf(value)).intValue();
    }

    private int encodeNumericValue(Object value, ModbusRegisterDefinition definition) {
        if (value == null) {
            return 0;
        }

        // 优先使用 Codec + ByteLayout 进行编码（与 decode 侧 scale 对称：先逆缩放再编码）
        if (definition.getCodec() != null && !definition.getCodec().isEmpty()) {
            Codec<?> codec = Codecs.getNow(definition.getCodec());
            ByteBuf buf = Unpooled.buffer(2);
            Object rawForCodec = inverseScaleForEncode(value, definition);
            Object toEncode = convertToCodecType(rawForCodec, codec.forType());
            @SuppressWarnings("unchecked")
            Codec<Object> rawCodec = (Codec<Object>) codec;
            rawCodec.encode(toEncode, buf);

            ByteLayout layout = resolveLayout(definition.getLayout());
            if (layout != null && layout.byteLength() <= buf.readableBytes()) {
                ByteBuf slice = buf.slice(0, layout.byteLength());
                layout.reorder(slice);
            }

            if (buf.readableBytes() >= 2) {
                return buf.readUnsignedShort();
            }
            if (buf.readableBytes() == 1) {
                return buf.readUnsignedByte();
            }
            return 0;
        }

        // 兼容旧配置: 使用 scaleFactor/scale 进行编码(不再处理有符号位)
        BigDecimal decimal = new BigDecimal(String.valueOf(value));
        if (definition.getScale() >= 0) {
            decimal = decimal.setScale(definition.getScale(), RoundingMode.HALF_UP);
        }
        double scaled = decimal.doubleValue();
        if (definition.getScaleFactor() != 0D && definition.getScaleFactor() != 1D) {
            scaled = scaled / definition.getScaleFactor();
        }
        return (int) scaled;
    }

    /**
     * 写入值逆变换为 Codec 原始量：与 {@link ModbusRegisterDefinition#decode} 中 scale 语义对称。
     */
    private static Object inverseScaleForEncode(Object value, ModbusRegisterDefinition definition) {
        if (value == null) {
            return null;
        }
        if (definition.getScaleFactor() == 1D && definition.getScale() < 0) {
            return value;
        }
        if (!(value instanceof Number)) {
            return value;
        }
        BigDecimal decimal = new BigDecimal(String.valueOf(value));
        if (definition.getScale() >= 0) {
            decimal = decimal.setScale(definition.getScale(), RoundingMode.HALF_UP);
        }
        double scaled = decimal.doubleValue();
        if (definition.getScaleFactor() != 0D && definition.getScaleFactor() != 1D) {
            scaled = scaled / definition.getScaleFactor();
        }
        return scaled;
    }

    /**
     * 根据寄存器定义, 使用 Codec 将当前寄存器值解码为完整结果。
     */
    private Object decodeByDefinition(int registerValue, ModbusRegisterDefinition definition) {
        if (definition.getCodec() == null || definition.getCodec().isEmpty()) {
            return registerValue;
        }
        ByteBuf buf = Unpooled.buffer(2);
        buf.writeShort(registerValue & 0xFFFF);
        try {
            return definition.decode(buf);
        } finally {
            buf.release();
        }
    }

    /**
     * 在数组或集合中更新指定下标的元素。
     */
    @SuppressWarnings("all")
    private Object applyElementUpdate(Object aggregate, int index, Object newValue) {
        if (aggregate == null || index < 0) {
            return aggregate;
        }
        Class<?> type = aggregate.getClass();
        if (type.isArray()) {
            int len = java.lang.reflect.Array.getLength(aggregate);
            if (index >= len) {
                return aggregate;
            }
            Class<?> componentType = type.getComponentType();
            Object converted = convertToCodecType(newValue, componentType);
            java.lang.reflect.Array.set(aggregate, index, converted);
            return aggregate;
        }
        if (aggregate instanceof List) {
            List list = (List) aggregate;
            if (index >= list.size()) {
                return aggregate;
            }
            Object converted = convertToCodecType(newValue, Object.class);
            list.set(index, converted);
            return aggregate;
        }
        return aggregate;
    }

    @SuppressWarnings("all")
    private Object convertToCodecType(Object value, Class<?> type) {
        if (value == null || type == null) {
            return value;
        }
        if (type.isInstance(value)) {
            return value;
        }
        return ConverterUtils.convert(value, type);
    }

    private ByteLayout resolveLayout(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return ByteLayouts.get(id).orElse(null);
    }

}
