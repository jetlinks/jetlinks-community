package org.jetlinks.community.device.function;

import lombok.AllArgsConstructor;
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
import reactor.core.publisher.Flux;

import java.util.Map;

/**
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

        return context -> provider
            .applyCondition(spec,
                            context,
                            deviceRepository
                                .createQuery()
                                .select(DeviceInstanceEntity::getId)
                                .nest())
            .flatMapMany(ctd -> ctd
                .end()
                .fetch()
                .map(DeviceInstanceEntity::getId))
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
