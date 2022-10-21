package org.jetlinks.community.network.manager.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.events.EntityBeforeDeleteEvent;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkProperties;
import org.jetlinks.community.network.manager.entity.CertificateEntity;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.jetlinks.community.reference.DataReferenceManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Component
@AllArgsConstructor
public class NetworkEntityEventHandler {

    private final NetworkConfigService networkService;

    private final DataReferenceManager referenceManager;

    private final NetworkManager networkManager;

    //禁止删除已有网络组件使用的证书
    @EventListener
    public void handleCertificateDelete(EntityBeforeDeleteEvent<CertificateEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(e -> networkService
                    .createQuery()
                    // FIXME: 2021/9/13 由于网络组件没有直接记录证书，还有更好的处理办法？
                    .$like$(NetworkConfigEntity::getConfiguration, e.getId())
                    .or()
                    .$like$(NetworkConfigEntity::getCluster, e.getId())
                    .count()
                    .doOnNext(i -> {
                        if (i > 0) {
                            throw new BusinessException("error.certificate_has_bean_use_by_network");
                        }
                    })
                )
        );
    }

    //禁止删除已有网关使用的网络组件
    @EventListener
    public void handleNetworkDelete(EntityBeforeDeleteEvent<NetworkConfigEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(e -> referenceManager.assertNotReferenced(DataReferenceManager.TYPE_NETWORK, e.getId()))
        );

    }


    @EventListener
    public void handleNetworkCreated(EntityCreatedEvent<NetworkConfigEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMapIterable(NetworkConfigEntity::toNetworkPropertiesList)
                .flatMap(this::networkConfigValidate)
                .then(handleEvent(event.getEntity()))
        );
    }

    @EventListener
    public void handleNetworkSaved(EntitySavedEvent<NetworkConfigEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .filter(conf -> conf.getConfiguration() != null || conf.getCluster() != null)
                .flatMapIterable(NetworkConfigEntity::toNetworkPropertiesList)
                .flatMap(this::networkConfigValidate)
                .then(handleEvent(event.getEntity()))
        );
    }

    @EventListener
    public void handleNetworkModify(EntityModifyEvent<NetworkConfigEntity> event) {
        event.async(
            Flux.fromIterable(event.getAfter())
                .filter(conf -> conf.getConfiguration() != null || conf.getCluster() != null)
                .flatMapIterable(NetworkConfigEntity::toNetworkPropertiesList)
                .flatMap(this::networkConfigValidate)
                .then(handleEvent(event.getAfter()))
        );
    }


    //网络组件配置验证
    private Mono<Void> networkConfigValidate(NetworkProperties properties) {
        return Mono.justOrEmpty(networkManager.getProvider(properties.getType()))
                   .flatMap(networkProvider -> networkProvider.createConfig(properties))
                   .then();
    }

    private Mono<Void> handleEvent(Collection<NetworkConfigEntity> entities) {
        return Flux
            .fromIterable(entities)
            .filter(conf -> conf.getState() == NetworkConfigState.enabled)
            .flatMap(conf -> networkManager.reload(conf.lookupNetworkType(), conf.getId()))
            .then();
    }


}
