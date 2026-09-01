package org.jetlinks.community.things.helper.modbus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.monitor.Monitor;

import java.nio.ByteOrder;
import java.util.*;

/**
 * 描述一个物模型在 Modbus 协议下的整体映射配置。
 * <p>
 * 优化内存缓存结构：
 * <ul>
 *   <li>使用索引结构加速查询（属性ID、寄存器键）</li>
 *   <li>懒加载索引，减少初始化开销</li>
 *   <li>使用不可变集合，降低内存占用</li>
 *   <li>提供快速查找方法</li>
 * </ul>
 *
 * @author zhou
 */
@Getter
@Setter
@NoArgsConstructor
public class ModbusThingsMapping {

    /**
     * 物模型标识(例如设备ID或物类型ID)
     */
    private String thingId;

    /**
     * 链路封装：TCP（含 MBAP）、RTU（含 CRC）或仅 PDU（默认，与历史配置一致）。
     */
    private ModbusLinkType modbusLinkType = ModbusLinkType.PDU;

    /**
     * 物模型属性与寄存器映射集合；寄存器定义（地址、Codec、数量等）内嵌在每条 {@link ModbusPropertyMapping#getRegister()} 中。
     */
    private List<ModbusPropertyMapping> properties = Collections.emptyList();

    public void setProperties(List<ModbusPropertyMapping> properties) {
        if (properties == null || properties.isEmpty()) {
            this.properties = Collections.emptyList();
            return;
        }
        List<ModbusPropertyMapping> list = new ArrayList<>(properties.size());
        for (ModbusPropertyMapping property : properties) {
            if (property != null && property.getProperty() != null && !property.getProperty().isEmpty()) {
                list.add(property);
            }
        }
        this.properties = Collections.unmodifiableList(list);
    }

    // 索引结构（懒加载）
    @JsonIgnore
    private transient volatile Map<String, ModbusPropertyMapping> propertyIndex;
    @JsonIgnore
    private transient volatile Map<ModbusRegisterKey, List<ModbusPropertyMapping>> registerIndex;

    /**
     * 初始化索引，在首次查询时自动调用
     */
    private void buildIndexes() {
        if (propertyIndex != null && registerIndex != null) {
            return;
        }

        synchronized (this) {
            if (propertyIndex != null && registerIndex != null) {
                return;
            }

            Map<String, ModbusPropertyMapping> propIdx = new HashMap<>(properties.size());
            Map<ModbusRegisterKey, List<ModbusPropertyMapping>> regIdx = new HashMap<>();

            for (ModbusPropertyMapping property : properties) {
                if (property == null || property.getProperty() == null) {
                    continue;
                }

                // 构建属性索引
                propIdx.put(property.getProperty(), property);

                // 构建寄存器索引
                if (property.getRegister() != null && property.getRegister().getKey() != null) {
                    ModbusRegisterKey key = property.getRegister().getKey();
                    regIdx.computeIfAbsent(key, k -> new ArrayList<>()).add(property);
                }
            }

            // 将列表转为不可变，减少内存占用
            Map<ModbusRegisterKey, List<ModbusPropertyMapping>> immutableRegIdx = new HashMap<>(regIdx.size());
            regIdx.forEach((k, v) -> immutableRegIdx.put(k, Collections.unmodifiableList(v)));

            this.propertyIndex = Collections.unmodifiableMap(propIdx);
            this.registerIndex = Collections.unmodifiableMap(immutableRegIdx);
        }
    }

    /**
     * 获取属性映射列表
     *
     * @return 不可变的属性映射列表
     */
    public List<ModbusPropertyMapping> getProperties() {
        return properties != null ? properties : Collections.emptyList();
    }

    /**
     * 根据属性ID快速查找映射配置（O(1)复杂度）
     *
     * @param propertyId 属性ID
     * @return 属性映射，如果不存在返回 Optional.empty()
     */
    public Optional<ModbusPropertyMapping> getProperty(String propertyId) {
        if (propertyId == null || propertyId.isEmpty()) {
            return Optional.empty();
        }
        ensureIndexes();
        return Optional.ofNullable(propertyIndex.get(propertyId));
    }

    /**
     * 根据寄存器键快速查找所有关联的属性映射（O(1)复杂度）
     *
     * @param key 寄存器键
     * @return 属性映射列表，如果不存在返回空列表
     */
    public List<ModbusPropertyMapping> getPropertiesByRegister(ModbusRegisterKey key) {
        if (key == null) {
            return Collections.emptyList();
        }
        ensureIndexes();
        return registerIndex.getOrDefault(key, Collections.emptyList());
    }

    /**
     * 检查是否包含指定属性
     *
     * @param propertyId 属性ID
     * @return true if contains
     */
    public boolean containsProperty(String propertyId) {
        if (propertyId == null || propertyId.isEmpty()) {
            return false;
        }
        ensureIndexes();
        return propertyIndex.containsKey(propertyId);
    }

    /**
     * 获取所有属性ID
     *
     * @return 属性ID集合
     */
    public Set<String> getPropertyIds() {
        ensureIndexes();
        return propertyIndex.keySet();
    }

    /**
     * 获取映射的寄存器键集合
     *
     * @return 寄存器键集合
     */
    public Set<ModbusRegisterKey> getRegisterKeys() {
        ensureIndexes();
        return registerIndex.keySet();
    }

    /**
     * 获取属性映射数量
     *
     * @return 属性数量
     */
    public int getPropertyCount() {
        return properties != null ? properties.size() : 0;
    }

    /**
     * 确保索引已构建
     */
    private void ensureIndexes() {
        if (propertyIndex == null || registerIndex == null) {
            buildIndexes();
        }
    }

    /**
     * 单帧线圈/离散解析时上限，防止异常报文导致过大遍历。
     */
    private static final int MAX_COIL_BITS_PER_DECODE = 2000;

    /**
     * 根据已解析的 {@link ModbusFrame}，按本映射配置直接解析出物模型属性值。
     * <p>
     * 不再经过整帧 → {@code Map<ModbusRegisterKey, Integer>} 的中间寄存器表，而是按每个属性映射
     * 在帧数据区中定位原始字节并调用 {@link ModbusRegisterDefinition#decode(ByteBuf)}。
     *
     * @param frame   逻辑 Modbus 帧（与 {@link ModbusFrameCodec#decode(ByteBuf)} 结果一致）
     * @param monitor 可为 null；解码单属性失败时记录告警
     * @return 属性 id → 属性值，无匹配时返回空 Map
     */
    public Map<String, Object> decode(ModbusFrame frame, Monitor monitor) {
        if (frame == null) {
            return Collections.emptyMap();
        }
        ensureIndexes();
        List<ModbusPropertyMapping> list = getProperties();
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (ModbusPropertyMapping pm : list) {
            if (pm == null || !pm.isReadable() || pm.getProperty() == null) {
                continue;
            }
            ModbusRegisterDefinition def = pm.getRegister();
            if (def == null || def.getKey() == null) {
                continue;
            }
            ModbusRegisterKey key = def.getKey();
            if (key.getSlaveId() != frame.getSlaveId()) {
                continue;
            }
            if (!functionMatchesFrame(key, frame.getFunctionCode())) {
                continue;
            }
            try {
                ByteBuf raw = extractPropertyRawFromFrame(frame, def);
                if (raw == null || !raw.isReadable()) {
                    continue;
                }
                Object decoded = def.decode(raw);
                if (decoded == null && (def.getCodec() == null || def.getCodec().isEmpty())) {
                    // 兼容线圈: 未配置 codec 时，如果是线圈类型且只有1字节数据，尝试转为 Boolean
                    ModbusRegisterType type = key.getType();
                    if ((type == ModbusRegisterType.Coils || type == ModbusRegisterType.DiscreteInputs)
                        && raw.readableBytes() == 1) {
                        decoded = raw.readByte() != 0;
                    }
                }
                Object value = pm.isBitOperation()
                    ? applyPropertyMapping(pm, decoded)
                    : applyPropertyMapping(pm, def.applyScaleAfterDecode(decoded));
                if (value != null) {
                    result.put(pm.getProperty(), value);
                }
            } catch (Exception ex) {
                if (monitor != null) {
                    monitor.logger().warn("modbus.mapping.decode.property.failed", pm.getProperty(), ex);
                }
            }
        }
        return result.isEmpty() ? Collections.emptyMap() : result;
    }

    /**
     * 从寄存器键值快照解析属性（兼容基于快照的读改写等路径），语义与 {@link #decode(ModbusFrame, Monitor)} 对齐。
     *
     * @param registers 寄存器当前值
     * @param monitor   可为 null
     * @return 属性 id → 属性值
     */
    public Map<String, Object> decodeRegisters(Map<ModbusRegisterKey, Integer> registers, Monitor monitor) {
        if (registers == null || registers.isEmpty()) {
            return Collections.emptyMap();
        }
        ensureIndexes();
        List<ModbusPropertyMapping> list = getProperties();
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (ModbusPropertyMapping pm : list) {
            if (pm == null || !pm.isReadable() || pm.getProperty() == null) {
                continue;
            }
            ModbusRegisterDefinition def = pm.getRegister();
            if (def == null || def.getKey() == null) {
                continue;
            }
            try {
                ByteBuf raw = extractPropertyRawFromRegisterMap(def, registers);
                if (raw == null || !raw.isReadable()) {
                    continue;
                }
                Object decoded = def.decode(raw);
                Object value = pm.isBitOperation()
                    ? applyPropertyMapping(pm, decoded)
                    : applyPropertyMapping(pm, def.applyScaleAfterDecode(decoded));
                if (value != null) {
                    result.put(pm.getProperty(), value);
                }
            } catch (Exception ex) {
                if (monitor != null) {
                    monitor.logger().warn("modbus.mapping.decode.property.failed", pm.getProperty(), ex);
                }
            }
        }
        return result.isEmpty() ? Collections.emptyMap() : result;
    }

    private static boolean functionMatchesFrame(ModbusRegisterKey key, int functionCode) {
        ModbusRegisterType t = key.getType();
        if (t == null) {
            return false;
        }
        return functionCode == t.getReadFunction()
            || (t.getWriteSingleFunction() != null && functionCode == t.getWriteSingleFunction())
            || (t.getWriteMultipleFunction() != null && functionCode == t.getWriteMultipleFunction())
            || (t.getMaskWriteFunction() != null && functionCode == t.getMaskWriteFunction());
    }

    /**
     * 数据区第一个线圈/寄存器对应的 PDU 地址（与透传场景下读响应数据区从地址 0 起算的约定一致）。
     */
    private static int pduDataStart(ModbusFrame frame) {
        int fc = frame.getFunctionCode();
        switch (fc) {
            case 0x01:
            case 0x02:
            case 0x03:
            case 0x04:
                return 0;
            case 0x05:
            case 0x06:
            case 0x0F:
            case 0x10:
            case 0x16:
                return frame.getAddress();
            default:
                return 0;
        }
    }

    private ByteBuf extractPropertyRawFromFrame(ModbusFrame frame, ModbusRegisterDefinition def) {
        ModbusRegisterKey key = def.getKey();
        ModbusRegisterType type = key.getType();
        int fc = frame.getFunctionCode();
        ByteBuf data = frame.getValues();
        int regAddr = key.getAddress();
        int count = Math.max(1, def.getRegisterCount());

        if (type == ModbusRegisterType.Coils || type == ModbusRegisterType.DiscreteInputs) {
            if (fc == 0x05) {
                if (regAddr != frame.getAddress()) {
                    return null;
                }
                if (data == null || data.readableBytes() < 2) {
                    return null;
                }
                int v = data.getUnsignedShort(data.readerIndex());
                int bitVal = (v == 0xFF00) ? 1 : 0;
                return Unpooled.buffer(2).writeShort(bitVal);
            }
            return extractCoilRegionFromFrame(frame, data, regAddr, count);
        }
        if (type == ModbusRegisterType.HoldingRegisters || type == ModbusRegisterType.InputRegisters) {
            return extractWordRegionFromFrame(frame, data, regAddr, count);
        }
        return null;
    }

    private static ByteBuf extractWordRegionFromFrame(ModbusFrame frame, ByteBuf data, int regAddr, int registerCount) {
        if (data == null || !data.isReadable()) {
            return null;
        }
        int pduStart = pduDataStart(frame);
        int rel = regAddr - pduStart;
        if (rel < 0) {
            return null;
        }
        int need = registerCount * 2;
        int off = rel * 2;
        if (data.readableBytes() < off + need) {
            return null;
        }
        return data.slice(data.readerIndex() + off, need);
    }

    private static ByteBuf extractCoilRegionFromFrame(ModbusFrame frame, ByteBuf data, int coilStart, int numCoils) {
        if (data == null || !data.isReadable()) {
            return null;
        }
        int pduStart = pduDataStart(frame);
        int bitStart = coilStart - pduStart;
        if (bitStart < 0 || numCoils < 1 || bitStart + numCoils > MAX_COIL_BITS_PER_DECODE) {
            return null;
        }
        if (numCoils <= 16) {
            int v = 0;
            for (int i = 0; i < numCoils; i++) {
                if (readCoilBit(data, bitStart + i)) {
                    v |= (1 << i);
                }
            }
            int nBytes = (numCoils + 7) / 8;
            ByteBuf out = Unpooled.buffer(nBytes).order(ByteOrder.LITTLE_ENDIAN);
            if (nBytes == 1) {
                out.writeByte(v);
            } else {
                out.writeShort(v);
            }
            return out;
        }
        int nBytes = (numCoils + 7) / 8;
        ByteBuf out = Unpooled.buffer(nBytes);
        for (int b = 0; b < nBytes; b++) {
            int by = 0;
            for (int bit = 0; bit < 8; bit++) {
                int idx = b * 8 + bit;
                if (idx >= numCoils) {
                    break;
                }
                if (readCoilBit(data, bitStart + idx)) {
                    by |= (1 << bit);
                }
            }
            out.writeByte(by);
        }
        return out;
    }

    private static boolean readCoilBit(ByteBuf coilBytes, int bitIndex) {
        int byteIdx = bitIndex / 8;
        int bitInByte = bitIndex % 8;
        if (coilBytes.readableBytes() <= byteIdx) {
            return false;
        }
        int b = coilBytes.getUnsignedByte(coilBytes.readerIndex() + byteIdx);
        return (b & (1 << bitInByte)) != 0;
    }

    private ByteBuf extractPropertyRawFromRegisterMap(ModbusRegisterDefinition def,
                                                      Map<ModbusRegisterKey, Integer> registers) {
        ModbusRegisterKey key = def.getKey();
        ModbusRegisterType type = key.getType();
        int count = Math.max(1, def.getRegisterCount());
        int start = key.getAddress();

        if (type == ModbusRegisterType.HoldingRegisters || type == ModbusRegisterType.InputRegisters) {
            ByteBuf out = Unpooled.buffer(count * 2);
            for (int i = 0; i < count; i++) {
                ModbusRegisterKey k = new ModbusRegisterKey(key.getSlaveId(), type, start + i);
                Integer v = registers.get(k);
                if (v == null) {
                    return null;
                }
                out.writeShort(v & 0xFFFF);
            }
            return out;
        }
        if (type == ModbusRegisterType.Coils || type == ModbusRegisterType.DiscreteInputs) {
            if (count <= 16) {
                int v = 0;
                for (int i = 0; i < count; i++) {
                    ModbusRegisterKey k = new ModbusRegisterKey(key.getSlaveId(), type, start + i);
                    Integer bit = registers.get(k);
                    if (bit == null) {
                        return null;
                    }
                    v |= (bit & 1) << i;
                }
                return Unpooled.buffer(2).writeShort(v);
            }
            int nBytes = (count + 7) / 8;
            ByteBuf out = Unpooled.buffer(nBytes);
            for (int b = 0; b < nBytes; b++) {
                int by = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int idx = b * 8 + bit;
                    if (idx >= count) {
                        break;
                    }
                    ModbusRegisterKey k = new ModbusRegisterKey(key.getSlaveId(), type, start + idx);
                    Integer val = registers.get(k);
                    if (val == null) {
                        return null;
                    }
                    if ((val & 1) != 0) {
                        by |= (1 << bit);
                    }
                }
                out.writeByte(by);
            }
            return out;
        }
        return null;
    }

    private static Object applyPropertyMapping(ModbusPropertyMapping pm, Object decoded) {
        if (decoded == null) {
            return null;
        }
        Object current = decoded;
        Integer el = pm.getElementIndex();
        if (el != null && el >= 0) {
            Object elem = extractElement(current, el);
            if (elem == null) {
                return null;
            }
            current = elem;
        }
        if (pm.isBitOperation()) {
            int raw = toUInt16(current);
            int bi = pm.getBitIndex() != null ? pm.getBitIndex() : 0;
            int len = pm.getBitLengthSafe();
            int mask = (1 << len) - 1;
            int bits = (raw >> bi) & mask;
            return len == 1 ? (bits != 0) : bits;
        }
        if (current != null && current.getClass().isArray() && pm.getElementIndex() == null) {
            int len = java.lang.reflect.Array.getLength(current);
            int count = pm.getRegister().getRegisterCount();
            ModbusRegisterKey key = pm.getRegister().getKey();
            ModbusRegisterType type = key != null ? key.getType() : null;
            int expectedLen = (type == ModbusRegisterType.Coils || type == ModbusRegisterType.DiscreteInputs)
                ? count : count * 16;

            // bit_array（MSB-first BitArray）：Modbus 线圈报文为 LSB-first，需根据所选 codec 进行位映射。
            // lsb_bit_array：Codec 已按 LSB 展开。
            String codecId = pm.getRegister().getCodec();

            // 对于 bit_array，由于它是按字节解码的，位长度始终是 8 的倍数，需要根据 registerCount 裁剪
            // 对于其他 array 类型，如果长度超过了预期的寄存器所能承载的长度，也进行裁剪
            if (pm.getRegister().getCodec() != null && pm.getRegister().getCodec().endsWith("_array")) {
                if (len > expectedLen) {
                    return copyArray(current, expectedLen);
                }
            }
        }
        return current;
    }

    private static Object copyArray(Object array, int length) {
        Object newArray = java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), length);
        System.arraycopy(array, 0, newArray, 0, length);
        return newArray;
    }

    private static int toUInt16(Object decoded) {
        if (decoded instanceof Number) {
            return ((Number) decoded).intValue() & 0xFFFF;
        }
        if (decoded instanceof Boolean) {
            return ((Boolean) decoded) ? 1 : 0;
        }
        return 0;
    }

    private static Object extractElement(Object decoded, int index) {
        if (decoded instanceof List) {
            List<?> list = (List<?>) decoded;
            return index < list.size() ? list.get(index) : null;
        }
        if (decoded instanceof Object[]) {
            Object[] arr = (Object[]) decoded;
            return index < arr.length ? arr[index] : null;
        }
        if (decoded instanceof boolean[]) {
            boolean[] arr = (boolean[]) decoded;
            return index < arr.length ? arr[index] : null;
        }
        if (decoded instanceof byte[]) {
            byte[] arr = (byte[]) decoded;
            return index < arr.length ? (arr[index] & 0xFF) : null;
        }
        if (decoded instanceof int[]) {
            int[] arr = (int[]) decoded;
            return index < arr.length ? arr[index] : null;
        }
        if (decoded != null && decoded.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(decoded);
            if (index >= len) {
                return null;
            }
            return java.lang.reflect.Array.get(decoded, index);
        }
        return null;
    }

    /**
     * 创建配置构建器
     *
     * @param thingId 物模型ID
     * @return 构建器
     */
    public static Builder builder(String thingId) {
        return new Builder(thingId);
    }

    /**
     * 配置构建器，用于快速构建映射配置
     */
    public static class Builder {
        private final String thingId;
        private final List<ModbusPropertyMapping> properties = new ArrayList<>();

        public Builder(String thingId) {
            this.thingId = Objects.requireNonNull(thingId, "thingId cannot be null");
        }

        /**
         * 添加属性映射
         *
         * @param property 属性映射
         * @return this
         */
        public Builder addProperty(ModbusPropertyMapping property) {
            if (property != null) {
                properties.add(property);
            }
            return this;
        }

        /**
         * 批量添加属性映射
         *
         * @param properties 属性映射列表
         * @return this
         */
        public Builder addProperties(List<ModbusPropertyMapping> properties) {
            if (properties != null) {
                this.properties.addAll(properties);
            }
            return this;
        }

        /**
         * 构建映射配置
         *
         * @return 映射配置
         */
        public ModbusThingsMapping build() {
            ModbusThingsMapping mapping = new ModbusThingsMapping();
            mapping.setThingId(thingId);
            mapping.setProperties(Collections.unmodifiableList(new ArrayList<>(properties)));
            return mapping;
        }
    }

    /**
     * 克隆配置，用于设备级覆盖产品级配置
     *
     * @return 克隆的配置
     */
    public ModbusThingsMapping copy() {
        ModbusThingsMapping copy = new ModbusThingsMapping();
        copy.setThingId(this.thingId);
        copy.setProperties(this.properties);
        // 不复制索引，让克隆对象在首次使用时自行构建
        return copy;
    }

}
