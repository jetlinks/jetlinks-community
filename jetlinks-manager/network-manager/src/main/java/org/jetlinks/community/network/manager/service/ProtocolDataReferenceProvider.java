package org.jetlinks.community.network.manager.service;

import lombok.AllArgsConstructor;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.reference.DataReferenceInfo;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.community.reference.DataReferenceProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 消息协议的数据引用提供商.
 *
 * 返回被设备接入网关使用的消息协议
 *
 * @author zhangji 2022/4/12
 */
@Component
@AllArgsConstructor
public class ProtocolDataReferenceProvider implements DataReferenceProvider {
    private final DeviceGatewayService deviceGatewayService;

    @Override
    public String getId() {
        return DataReferenceManager.TYPE_PROTOCOL;
    }

    @Override
    public Flux<DataReferenceInfo> getReference(String protocolId) {
        return deviceGatewayService
            .createQuery()
            .where()
            .is(DeviceGatewayEntity::getProtocol, protocolId)
            .fetch()
            .map(e -> DataReferenceInfo.of(e.getId(),DataReferenceManager.TYPE_PROTOCOL, e.getProtocol(), e.getName()));
    }

    @Override
    public Flux<DataReferenceInfo> getReferences() {
        return deviceGatewayService
            .createQuery()
            .where()
            .notNull(DeviceGatewayEntity::getChannelId)
            .fetch()
            .map(e -> DataReferenceInfo.of(e.getId(),DataReferenceManager.TYPE_PROTOCOL, e.getChannelId(), e.getName()));
    }
}
