package org.jetlinks.community.network.manager.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Service
public class DeviceGatewayService extends GenericReactiveCrudService<DeviceGatewayEntity, String> {

    public Mono<Integer> updateState(String id, NetworkConfigState state) {
        return createUpdate().where()
            .and(DeviceGatewayEntity::getId, id)
            .set(DeviceGatewayEntity::getState, state)
            .execute();
    }

    @Override
    public Mono<SaveResult> save(Publisher<DeviceGatewayEntity> entityPublisher) {
        return super.save(
            Flux.from(entityPublisher)
                .doOnNext(entity -> {
                    if (StringUtils.isEmpty(entity.getId())) {
                        entity.setState(NetworkConfigState.disabled);
                    } else {
                        entity.setState(null);
                    }
                }));
    }
    @Override
    public Mono<Integer> insert(Publisher<DeviceGatewayEntity> entityPublisher) {
        return super.insert(Flux.from(entityPublisher)
            .doOnNext(deviceGatewayEntity -> deviceGatewayEntity.setState(NetworkConfigState.disabled)));
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return findById(Mono.from(idPublisher))
            .switchIfEmpty(Mono.error(()->new NotFoundException("改设备网关不存在")))
            .doOnNext(deviceGatewayEntity -> {
                if (NetworkConfigState.enabled.equals(deviceGatewayEntity.getState())) {
                    throw new UnsupportedOperationException("该设备网关已启用");
                }
            })
            .flatMap(deviceGatewayEntity -> super.deleteById(Mono.just(deviceGatewayEntity.getId())));
    }
}
