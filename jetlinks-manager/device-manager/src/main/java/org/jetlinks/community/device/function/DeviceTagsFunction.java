package org.jetlinks.community.device.function;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.core.device.DeviceThingType;
import org.jetlinks.core.things.ThingsDataManager;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 获取设备标签函数
 * <p>
 * select device.tags(deviceId,'tag1','tag2')
 *
 * @since 1.3
 */
@Component
public class DeviceTagsFunction extends FunctionMapFeature {

    public DeviceTagsFunction(ReactiveRepository<DeviceTagEntity, String> tagReposiotry,
                              ThingsDataManager dataManager) {

        super("device.tags", 100, 1, args ->
            args.collectList()
                .flatMap(list -> {
                    Object deviceId = list.get(0);
                    Set<Object> tags = list.stream().skip(1).collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));

                    //数据库加载
                    Flux<Tuple2<String, Object>> loaderFromDb
                        = Flux.defer(() -> tagReposiotry
                        .createQuery()
                        .where(DeviceTagEntity::getDeviceId, deviceId)
                        .when(!CollectionUtils.isEmpty(tags), query -> query.in(DeviceTagEntity::getKey, tags))
                        .fetch()
                        .map(e -> Tuples.of(e.getKey(), e.getValue())));

                    //没有指定标签,从数据库加载全部.
                    if (tags.isEmpty()) {
                        return loaderFromDb.collectMap(Tuple2::getT1, Tuple2::getT2);
                    }

                    return Flux
                        .fromIterable(tags)
                        .flatMap(tag -> dataManager
                            //优先从缓存中获取
                            .getLastTag(DeviceThingType.device.getId(),
                                        String.valueOf(deviceId), String.valueOf(tag),
                                        System.currentTimeMillis())
                            .map(_tag -> {
                                tags.remove(_tag.getTag());
                                return Tuples.of(_tag.getTag(), _tag.getValue());
                            }))
                        .concatWith(Flux.defer(() -> {
                            if (!tags.isEmpty()) {
                                return loaderFromDb;
                            }
                            return Mono.empty();
                        }))
                        .collectMap(Tuple2::getT1, Tuple2::getT2);

                }));
    }
}
