package org.jetlinks.community.device.function;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.core.device.DeviceThingType;
import org.jetlinks.core.things.ThingTag;
import org.jetlinks.core.things.ThingsDataManager;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 获取设备标签函数
 * <p>
 * select device.tag(deviceId,'tag1')
 *
 * @since 1.9
 */
@Component
public class DeviceTagFunction extends FunctionMapFeature {

    public DeviceTagFunction(ReactiveRepository<DeviceTagEntity, String> tagReposiotry,
                             ThingsDataManager dataManager) {
        super("device.tag", 2, 2, args ->
            args.collectList()
                .flatMap(list -> {
                    Object deviceId = list.get(0);
                    Object tagKey = list.get(1);

                    return dataManager
                        //优先从缓存中获取
                        .getLastTag(DeviceThingType.device.getId(),
                                    String.valueOf(deviceId), String.valueOf(tagKey),
                                    System.currentTimeMillis())
                        .map(ThingTag::getValue)
                        .switchIfEmpty(Mono.defer(() -> tagReposiotry
                            .createQuery()
                            .where(DeviceTagEntity::getDeviceId, deviceId)
                            .and(DeviceTagEntity::getKey, tagKey)
                            .fetch()
                            .take(1)
                            .singleOrEmpty()
                            .map(DeviceTagEntity::getValue)));
                }));
    }
}
