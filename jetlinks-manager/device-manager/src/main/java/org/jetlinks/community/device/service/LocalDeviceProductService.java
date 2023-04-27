package org.jetlinks.community.device.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceProductState;
import org.jetlinks.community.device.events.DeviceProductDeployEvent;
import org.jetlinks.core.device.DeviceRegistry;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
            .doOnNext(this::validateDeviceProduct)
            .flatMap(product -> registry
                .register(product.toProductInfo())
                .onErrorMap(e -> new BusinessException("error.unable_to_load_protocol_by_access_id", 404, product.getMessageProtocol()))
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

    private void validateDeviceProduct(DeviceProductEntity product) {
        // 设备接入ID不能为空
//        Assert.hasText(product.getAccessId(), "error.access_id_can_not_be_empty");
        // 发布前，必须填写消息协议
        Assert.hasText(product.getMessageProtocol(), "error.please_select_the_access_mode_first");
    }


    public Mono<Integer> cancelDeploy(String id) {
        return createUpdate()
            .set(DeviceProductEntity::getState, DeviceProductState.unregistered.getValue())
            .where(DeviceProductEntity::getId, id)
            .execute()
            .flatMap(integer ->
                         registry
                             .unregisterProduct(id)
                             .thenReturn(integer));

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
