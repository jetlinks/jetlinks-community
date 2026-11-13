package org.jetlinks.community.things.helper.modbus;

import org.jetlinks.core.monitor.Monitor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 {@link ModbusThingsMapping} 的静态入口：寄存器快照解析、读/写请求构建。
 */
public final class ModbusThingsHelper {

    private ModbusThingsHelper() {
    }

    /**
     * 将寄存器当前值解析为物模型属性（内部委托 {@link ModbusThingsMapping#decodeRegisters(Map, Monitor)}）。
     */
    public static Map<String, Object> decodeProperties(ModbusThingsMapping mapping,
                                                       Map<ModbusRegisterKey, Integer> registers,
                                                       Monitor monitor) {
        if (mapping == null || registers == null || registers.isEmpty()) {
            return Collections.emptyMap();
        }
        return mapping.decodeRegisters(registers, monitor);
    }

    public static List<ModbusReadRequest> buildReadRequests(ModbusThingsMapping mapping,
                                                            Collection<String> properties) {
        return ModbusReadRequestBuilder.build(properties, mapping);
    }

    public static List<ModbusReadRequest> buildReadRequests(ModbusThingsMapping mapping,
                                                            Collection<String> properties,
                                                            Monitor monitor) {
        return buildReadRequests(mapping, properties);
    }

    public static List<ModbusReadRequest> buildReadRequests(ModbusThingsMapping mapping,
                                                            Map<String, ?> properties,
                                                            Monitor monitor) {
        return ModbusReadRequestBuilder.build(properties, mapping);
    }

    public static List<ModbusWriteRequest> buildWriteRequests(ModbusThingsMapping mapping,
                                                              Map<String, Object> properties,
                                                              ModbusRegisterSnapshot snapshot,
                                                              Monitor monitor) {
        if (mapping == null || properties == null || properties.isEmpty()) {
            return List.of();
        }
        List<ThingWriteOperation> ops = properties.entrySet().stream()
            .map(e -> new ThingWriteOperation(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
        return new ModbusWriteRequestBuilder().build(ops, mapping, snapshot);
    }
}
