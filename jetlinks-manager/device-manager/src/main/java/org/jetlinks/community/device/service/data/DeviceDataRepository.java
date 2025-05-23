/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
