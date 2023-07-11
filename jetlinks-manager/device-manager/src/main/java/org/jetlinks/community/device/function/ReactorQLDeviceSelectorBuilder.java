package org.jetlinks.community.device.function;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.core.NestConditional;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.rule.engine.executor.DeviceSelector;
import org.jetlinks.community.rule.engine.executor.DeviceSelectorBuilder;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProvider;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProviders;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;
import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.feature.FromFeature;
import org.springframework.data.util.Lazy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 基于ReactorQL的设备选择器,通过自定义{@link FromFeature}来实现设备数据源.
 * <pre>
 * in_gourp('groupId') 在指定的设备分组中
 * in_group_tree('groupId') 在指定分组中（包含下级分组）
 * same_group('deviceId') 在指定设备的相同分组中
 * product('productId') 指定产品ID对应的设备
 * tag('tag1Key','tag1Value','tag2Key','tag2Value') 按指定的标签获取
 * state('online') 按指定的状态获取
 * in_tenant('租户ID') 在指定租户中的设备
 * </pre>
 *
 * @author zhouhao
 * @since 2.0
 */
@AllArgsConstructor
public class ReactorQLDeviceSelectorBuilder implements DeviceSelectorBuilder {

    private final DeviceRegistry registry;

    private final ReactiveRepository<DeviceInstanceEntity, String> deviceRepository;


    @Override
    @SuppressWarnings("all")
    public DeviceSelector createSelector(DeviceSelectorSpec spec) {
        DeviceSelectorProvider provider = DeviceSelectorProviders
            .getProvider(spec.getSelector())
            .orElseThrow(() -> new UnsupportedOperationException("unsupported selector:" + spec.getSelector()));

        //固定设备,直接获取,避免查询数据库性能低.
        if (DeviceSelectorProviders.isFixed(spec)) {
            return ctx -> {
                return spec
                    .resolveSelectorValues(ctx)
                    .map(String::valueOf)
                    .flatMap(registry::getDevice);
            };
        }

        BiFunction<NestConditional<ReactiveQuery<DeviceInstanceEntity>>,
            Map<String, Object>,
            Mono<NestConditional<ReactiveQuery<DeviceInstanceEntity>>>> function = provider
            .createLazy(spec);

        return context -> function
            .apply(deviceRepository
                       .createQuery()
                       .select(DeviceInstanceEntity::getId)
                       .nest(),
                   context
            )
            .flatMapMany(ctd -> ctd.end().fetch().map(DeviceInstanceEntity::getId))
            .flatMap(registry::getDevice);
    }

    @AllArgsConstructor
    static class ReactorQLDeviceSelector implements DeviceSelector {

        private final ReactorQL ql;

        private final DeviceRegistry registry;

        @Override
        public Flux<DeviceOperator> select(Map<String, Object> context) {
            return ql
                .start(
                    ReactorQLContext
                        .ofDatasource((r) -> Flux.just(context))
                        .bindAll(context)
                )
                .map(ReactorQLRecord::asMap)
                .flatMap(res -> registry.getDevice((String) res.get("id")))
                ;
        }
    }
}
