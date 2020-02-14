package org.jetlinks.community.device.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.ProductInfo;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceProductState;
import org.jetlinks.community.device.events.DeviceProductDeployEvent;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class LocalDeviceProductService extends GenericReactiveCrudService<DeviceProductEntity, String> {

    @Autowired
    private DeviceRegistry registry;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public Mono<Integer> deploy(String id) {
        return findById(Mono.just(id))
            .flatMap(product -> registry.registry(new ProductInfo(id, product.getMessageProtocol(), product.getMetadata()))
                .flatMap(deviceProductOperator -> deviceProductOperator.setConfigs(product.getConfiguration()))
                .flatMap(re -> createUpdate()
                    .set(DeviceProductEntity::getState, DeviceProductState.registered.getValue())
                    .where(DeviceProductEntity::getId, id)
                    .execute())
                .doOnNext(i -> {
                    log.debug("设备型号：{}发布成功", product.getName());
                    eventPublisher.publishEvent(FastBeanCopier.copy(product, new DeviceProductDeployEvent()));
                })
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
        // TODO: 2019/12/5 校验是否可以删除
        return super.deleteById(idPublisher);
    }

}
