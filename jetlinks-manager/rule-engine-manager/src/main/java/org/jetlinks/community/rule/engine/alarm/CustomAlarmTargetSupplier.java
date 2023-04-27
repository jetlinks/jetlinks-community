package org.jetlinks.community.rule.engine.alarm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author bestfeng
 */
public class CustomAlarmTargetSupplier implements AlarmTargetSupplier {

    static Map<String, AlarmTarget> targets = new LinkedHashMap<>();

    public static CustomAlarmTargetSupplier defaultSupplier = new CustomAlarmTargetSupplier();

    static {
        register(new ProductAlarmTarget());
        register(new DeviceAlarmTarget());
        register(new OtherAlarmTarget());
    }

    public static void register(AlarmTarget target) {
        targets.put(target.getType(), target);
    }


    @Override
    public Optional<AlarmTarget> getByType(String type) {
        return Optional.ofNullable(targets.get(type));
    }

    @Override
    public Map<String, AlarmTarget> getAll() {
        return targets;
    }
}
