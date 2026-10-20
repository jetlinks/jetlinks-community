package org.jetlinks.community.things.helper.modbus;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 将物模型要读取的属性 ID 转换为 Modbus 读请求, 并自动按从站/功能码合并连续寄存器。
 * <p>
 * 全部为静态方法, 可直接传入 {@link Collection} 或 {@link Map}（仅使用 key 作为属性 ID）, 避免额外包装对象。
 */
public final class ModbusReadRequestBuilder {

    private ModbusReadRequestBuilder() {
    }

    /**
     * 根据属性 ID 集合与映射配置生成 Modbus 读请求。
     *
     * @param propertyIds 要读取的物模型属性 ID（null 或空字符串会被忽略）
     * @param mapping     映射配置
     * @return Modbus 读请求列表
     */
    public static List<ModbusReadRequest> build(Collection<String> propertyIds,
                                                ModbusThingsMapping mapping) {
        if (propertyIds == null || propertyIds.isEmpty() || mapping == null) {
            return List.of();
        }

        List<RegisterReadUnit> units = new ArrayList<>();
        for (String propertyId : propertyIds) {
            if (propertyId == null || propertyId.isEmpty()) {
                continue;
            }
            mapping
                .getProperty(propertyId)
                .ifPresent(prop -> {
                    ModbusRegisterDefinition def = prop.getRegister();
                    if (def == null || def.getKey() == null) {
                        return;
                    }
                    ModbusRegisterKey key = def.getKey();
                    int start = key.getAddress();
                    int quantity = Math.max(def.getRegisterCount(), 1);
                    units.add(new RegisterReadUnit(key.getSlaveId(), key.getType(), start, quantity));
                });
        }

        if (units.isEmpty()) {
            return List.of();
        }

        Map<SlaveFunctionKey, List<RegisterReadUnit>> groups = units
            .stream()
            .collect(Collectors.groupingBy(unit -> new SlaveFunctionKey(unit.getSlaveId(), unit.getType())));

        List<ModbusReadRequest> requests = new ArrayList<>();
        for (Map.Entry<SlaveFunctionKey, List<RegisterReadUnit>> entry : groups.entrySet()) {
            List<RegisterReadUnit> groupUnits = entry
                .getValue()
                .stream()
                .sorted(Comparator.comparingInt(RegisterReadUnit::getAddress))
                .collect(Collectors.toList());

            mergeGroup(entry.getKey(), groupUnits, requests);
        }
        return requests;
    }

    /**
     * 根据属性 Map 与映射配置生成 Modbus 读请求。
     * <p>
     * 仅使用 {@link Map#keySet()} 作为要读取的属性 ID，忽略 value。
     *
     * @param properties 属性 ID 到任意值的映射（value 不参与计算）
     * @param mapping    映射配置
     * @return Modbus 读请求列表
     */
    public static List<ModbusReadRequest> build(Map<String, ?> properties,
                                                ModbusThingsMapping mapping) {
        if (properties == null || properties.isEmpty()) {
            return List.of();
        }
        return build(properties.keySet(), mapping);
    }

    private static void mergeGroup(SlaveFunctionKey key,
                                   List<RegisterReadUnit> units,
                                   List<ModbusReadRequest> output) {
        if (units.isEmpty()) {
            return;
        }
        int currentStart = units.get(0).getAddress();
        int currentEnd = currentStart + units.get(0).getQuantity() - 1;

        for (int i = 1; i < units.size(); i++) {
            RegisterReadUnit unit = units.get(i);
            int start = unit.getAddress();
            int end = start + unit.getQuantity() - 1;
            // 地址连续则合并
            if (start <= currentEnd + 1) {
                currentEnd = Math.max(currentEnd, end);
            } else {
                output.add(new ModbusReadRequest(key.getSlaveId(), key.getFunctionCode(), currentStart, currentEnd - currentStart + 1));
                currentStart = start;
                currentEnd = end;
            }
        }
        output.add(new ModbusReadRequest(key.getSlaveId(), key.getFunctionCode(), currentStart, currentEnd - currentStart + 1));
    }

    @Getter
    @AllArgsConstructor
    private static class RegisterReadUnit {
        private final int slaveId;
        private final ModbusRegisterType type;
        private final int address;
        private final int quantity;
    }

    @Getter
    @AllArgsConstructor
    private static class SlaveFunctionKey {
        private final int slaveId;
        private final ModbusRegisterType type;

        public int getFunctionCode() {
            return type != null ? type.getReadFunction() : 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SlaveFunctionKey that = (SlaveFunctionKey) o;
            return slaveId == that.slaveId && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(slaveId, type);
        }
    }

}
