package org.jetlinks.community.network.manager.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.enums.DeviceGatewayState;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import static org.jetlinks.community.network.manager.service.DeviceGatewayEventHandler.DO_NOT_RELOAD_GATEWAY;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Service
public class DeviceGatewayService extends GenericReactiveCrudService<DeviceGatewayEntity, String> {

    public Mono<Integer> updateState(String id, DeviceGatewayState state) {
        return createUpdate()
            .where()
            .and(DeviceGatewayEntity::getId, id)
            .set(DeviceGatewayEntity::getState, state)
            .set(DeviceGatewayEntity::getStateTime, System.currentTimeMillis())
            .execute()
            .contextWrite(Context.of(DO_NOT_RELOAD_GATEWAY, true));
    }


    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return this
            .findById(Flux.from(idPublisher))
            .switchIfEmpty(Mono.error(new NotFoundException("error.device_gateway_not_exist")))
            .doOnNext(deviceGatewayEntity -> {
                if (DeviceGatewayState.enabled.equals(deviceGatewayEntity.getState())) {
                    throw new UnsupportedOperationException("error.device_gateway_enabled");
                }
            })
            .map(DeviceGatewayEntity::getId)
            .as(super::deleteById);
    }
}
