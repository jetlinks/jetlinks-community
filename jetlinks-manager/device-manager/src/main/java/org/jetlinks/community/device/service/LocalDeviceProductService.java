package org.jetlinks.community.device.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.ProductInfo;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceProductState;
import org.jetlinks.community.device.events.DeviceProductDeployEvent;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.jetlinks.community.device.enums.DeviceType.gateway;

@Service
@Slf4j
public class LocalDeviceProductService extends GenericReactiveCrudService<DeviceProductEntity, String> {

    @Autowired
    private DeviceRegistry registry;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ReactiveRepository<DeviceInstanceEntity, String> instanceRepository;

    public Mono<Integer> deploy(String id) {
        return findById(Mono.just(id))
            .flatMap(product -> registry
                .register(product.toProductInfo())
                .then(
                    createUpdate()
                        .set(DeviceProductEntity::getState, DeviceProductState.registered.getValue())
                        .where(DeviceProductEntity::getId, id)
                        .execute()
                )
                .flatMap(i -> FastBeanCopier
                    .copy(product, new DeviceProductDeployEvent())
                    .publish(eventPublisher)
                    .thenReturn(i))
            );
    }


    public Mono<Integer> cancelDeploy(String id) {
        return createUpdate()
            .set(DeviceProductEntity::getState, DeviceProductState.unregistered.getValue())
            .where(DeviceProductEntity::getId, id)
            .execute();

    }


    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
            .collectList()
            .flatMap(idList ->
                instanceRepository.createQuery()
                    .where()
                    .in(DeviceInstanceEntity::getProductId, idList)
                    .count()
                    .flatMap(i -> {
                        if (i > 0) {
                            return Mono.error(new IllegalArgumentException("存在关联设备,无法删除!"));
                        } else {
                            return super.deleteById(Flux.fromIterable(idList));
                        }
                    }));
    }

}
