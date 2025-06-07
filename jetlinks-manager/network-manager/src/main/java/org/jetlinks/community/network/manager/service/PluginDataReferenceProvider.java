package org.jetlinks.community.network.manager.service;

import lombok.AllArgsConstructor;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.reference.DataReferenceInfo;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.community.reference.DataReferenceProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 插件的数据引用提供商.
 *
 * 返回被设备接入网关使用的插件
 *
 * @author zhangji 2022/4/12
 */
@Component
@AllArgsConstructor
public class PluginDataReferenceProvider implements DataReferenceProvider {
    private final DeviceGatewayService deviceGatewayService;

    @Override
    public String getId() {
        return DataReferenceManager.TYPE_PLUGIN;
    }

    @Override
    public Flux<DataReferenceInfo> getReference(String pluginId) {
        return deviceGatewayService
            .createQuery()
            .where(DeviceGatewayEntity::getChannel, DataReferenceManager.TYPE_PLUGIN)
            .is(DeviceGatewayEntity::getChannelId, pluginId)
            .fetch()
            .map(e -> DataReferenceInfo.of(e.getChannelId(),DataReferenceManager.TYPE_DEVICE_GATEWAY, e.getId(), e.getName()));
    }

    @Override
    public Flux<DataReferenceInfo> getReferences() {
        return deviceGatewayService
            .createQuery()
            .where(DeviceGatewayEntity::getChannel, DataReferenceManager.TYPE_PLUGIN)
            .fetch()
            .map(e -> DataReferenceInfo.of(e.getChannelId(),DataReferenceManager.TYPE_DEVICE_GATEWAY, e.getId(), e.getName()));
    }
}
