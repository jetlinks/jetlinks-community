package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.reference.DataReferenceInfo;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.community.reference.DataReferenceProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Component
@AllArgsConstructor
public class ProductReferenceDeviceGatewayProvider implements DataReferenceProvider {

    private final ReactiveRepository<DeviceProductEntity, String> repository;

    @Override
    public String getId() {
        return DataReferenceManager.TYPE_DEVICE_GATEWAY;
    }

    @Override
    public Flux<DataReferenceInfo> getReference(String dataId) {
        return repository
            .createQuery()
            .where(DeviceProductEntity::getAccessId, dataId)
            .fetch()
            .map(e -> DataReferenceInfo
                .of(e.getAccessId(),
                    DataReferenceManager.TYPE_DEVICE_GATEWAY,
                    e.getId(),
                    e.getName()));
    }

    @Override
    public Flux<DataReferenceInfo> getReferences() {
        return repository
            .createQuery()
            .fetch()
            .filter(e -> StringUtils.hasText(e.getAccessId()))
            .map(e -> DataReferenceInfo
                .of(e.getAccessId(),
                    DataReferenceManager.TYPE_DEVICE_GATEWAY,
                    e.getId(),
                    e.getName()));
    }
}
