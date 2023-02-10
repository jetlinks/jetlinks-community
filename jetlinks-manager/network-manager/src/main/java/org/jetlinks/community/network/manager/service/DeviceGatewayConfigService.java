package org.jetlinks.community.network.manager.service;

import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.gateway.supports.DeviceGatewayPropertiesManager;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.reference.DataReferenceInfo;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.community.reference.DataReferenceProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DeviceGatewayConfigService implements DeviceGatewayPropertiesManager, DataReferenceProvider {


    private final DeviceGatewayService deviceGatewayService;

    @Override
    public String getId() {
        return DataReferenceManager.TYPE_NETWORK;
    }

    @Override
    public Flux<DataReferenceInfo> getReference(String networkId) {
        return deviceGatewayService
            .createQuery()
            .where()
            .and(DeviceGatewayEntity::getChannel, DeviceGatewayProvider.CHANNEL_NETWORK)
            .is(DeviceGatewayEntity::getChannelId, networkId)
            .fetch()
            .map(e -> DataReferenceInfo.of(e.getId(),DataReferenceManager.TYPE_NETWORK, e.getChannelId(), e.getName()));
    }

    @Override
    public Flux<DataReferenceInfo> getReferences() {
        return deviceGatewayService
            .createQuery()
            .where()
            .and(DeviceGatewayEntity::getChannel, DeviceGatewayProvider.CHANNEL_NETWORK)
            .notNull(DeviceGatewayEntity::getChannelId)
            .fetch()
            .map(e -> DataReferenceInfo.of(e.getId(),DataReferenceManager.TYPE_NETWORK, e.getChannelId(), e.getName()));
    }

    public DeviceGatewayConfigService(DeviceGatewayService deviceGatewayService) {
        this.deviceGatewayService = deviceGatewayService;
    }

    @Override
    public Mono<DeviceGatewayProperties> getProperties(String id) {

        return deviceGatewayService
            .findById(id)
//            .switchIfEmpty(Mono.error(new NotFoundException("该设备网关不存在")))
            .map(DeviceGatewayEntity::toProperties);
    }


    @Override
    public Flux<DeviceGatewayProperties> getPropertiesByChannel(String channel) {
        return deviceGatewayService
            .createQuery()
            .where()
            .and(DeviceGatewayEntity::getChannel, channel)
            .fetch()
            .map(DeviceGatewayEntity::toProperties);
    }

}
