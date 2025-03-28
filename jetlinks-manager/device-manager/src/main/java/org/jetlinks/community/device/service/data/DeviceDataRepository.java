package org.jetlinks.community.device.service.data;

import org.jetlinks.community.things.data.operations.SaveOperations;
import org.jetlinks.community.things.data.operations.TemplateOperations;
import org.jetlinks.community.things.data.operations.ThingOperations;
import reactor.core.publisher.Mono;

/**
 * 设备数据仓库,用户存储和查询设备相关历史数据
 * @author zhouhao
 * @since 2.0
 * @see org.jetlinks.community.things.ThingsDataRepository
 * @see org.jetlinks.community.things.data.ThingsDataRepositoryStrategy
 */
public interface DeviceDataRepository {
    /**
     * @return 返回保存操作接口, 用于对物数据进行保存
     */
    SaveOperations opsForSave();

    /**
     * 返回设备操作接口
     *
     * @param deviceId   设备ID
     * @return 操作接口
     */
    Mono<ThingOperations> opsForDevice(String deviceId);

    /**
     * 返回产品操作接口.
     *
     * @param productId 产品ID
     * @return 操作接口
     */
    Mono<TemplateOperations> opsForProduct(String productId);
}
