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
package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.community.device.entity.DeviceMetadataMappingDetail;
import org.jetlinks.community.device.entity.DeviceMetadataMappingEntity;
import org.jetlinks.community.device.service.DeviceMetadataMappingService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/device/metadata/mapping")
@Resource(id = "device-mapping", name = "设备物模型映射")
@Tag(name = "设备物模型映射")
@AllArgsConstructor
public class DeviceMetadataMappingController {

    public final DeviceMetadataMappingService mappingService;

    @PatchMapping("/device/{deviceId}")
    @SaveAction
    @Operation(summary = "保存设备映射信息")
    public Mono<Void> saveDeviceMapping(@PathVariable @Parameter(description = "设备ID") String deviceId,
                                        @RequestBody Flux<DeviceMetadataMappingEntity> mappings) {
        return mappingService.saveDeviceMapping(deviceId, mappings);
    }

    @PatchMapping("/product/{productId}")
    @SaveAction
    @Operation(summary = "保存产品映射信息")
    public Mono<Void> saveProductMapping(@PathVariable @Parameter(description = "产品ID") String productId,
                                         @RequestBody Flux<DeviceMetadataMappingEntity> mappings) {
        return mappingService.saveProductMapping(productId, mappings);
    }

    @GetMapping("/product/{productId}")
    @QueryAction
    @Operation(summary = "获取产品映射信息")
    public Flux<DeviceMetadataMappingDetail> getProductMapping(@PathVariable
                                                               @Parameter(description = "产品ID") String productId) {
        return mappingService.getProductMappingDetail(productId);
    }

    @GetMapping("/device/{deviceId}")
    @QueryAction
    @Operation(summary = "获取设备映射信息")
    public Flux<DeviceMetadataMappingDetail> getDeviceMapping(@PathVariable
                                                              @Parameter(description = "设备ID") String deviceId) {
        return mappingService.getDeviceMappingDetail(deviceId);
    }

}
