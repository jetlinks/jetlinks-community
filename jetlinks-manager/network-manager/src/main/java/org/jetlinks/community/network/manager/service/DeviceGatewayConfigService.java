package org.jetlinks.community.network.manager.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.gateway.supports.DeviceGatewayPropertiesManager;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.enums.DeviceGatewayState;
import org.jetlinks.community.reference.DataReferenceInfo;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.community.reference.DataReferenceProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DeviceGatewayConfigService implements DeviceGatewayPropertiesManager, DataReferenceProvider {
    private final ReactiveRepository<DeviceGatewayEntity, String> repository;

    @Override
    public String getId() {
        return DataReferenceManager.TYPE_NETWORK;
    }

    @Override
    public Flux<DeviceGatewayProperties> getAllProperties() {
        return repository
            .createQuery()
            .where(DeviceGatewayEntity::getState, DeviceGatewayState.enabled)
            .fetch()
            .map(DeviceGatewayEntity::toProperties);
    }

    @Override
    public Flux<DataReferenceInfo> getReference(String networkId) {
        return repository
            .createQuery()
            .where()
            .and(DeviceGatewayEntity::getChannel, DeviceGatewayProvider.CHANNEL_NETWORK)
            .is(DeviceGatewayEntity::getChannelId, networkId)
            .fetch()
            .map(e -> DataReferenceInfo.of(e.getChannelId(), DataReferenceManager.TYPE_DEVICE_GATEWAY, e.getId(), e.getName()));
    }

    @Override
    public Flux<DataReferenceInfo> getReferences() {
        return repository
            .createQuery()
            .where()
            .and(DeviceGatewayEntity::getChannel, DeviceGatewayProvider.CHANNEL_NETWORK)
            .notNull(DeviceGatewayEntity::getChannelId)
            .fetch()
            .map(e -> DataReferenceInfo.of(e.getChannelId(), DataReferenceManager.TYPE_DEVICE_GATEWAY, e.getId(), e.getName()));
    }

    public DeviceGatewayConfigService(ReactiveRepository<DeviceGatewayEntity, String> repository) {
        this.repository = repository;
    }

    @Override
    public Mono<DeviceGatewayProperties> getProperties(String id) {

        return repository
            .findById(id)
            .map(DeviceGatewayEntity::toProperties);
    }


    @Override
    public Flux<DeviceGatewayProperties> getPropertiesByChannel(String channel) {
        return repository
            .createQuery()
            .where()
            .and(DeviceGatewayEntity::getChannel, channel)
            .fetch()
            .map(DeviceGatewayEntity::toProperties);
    }

}
