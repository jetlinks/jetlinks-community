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

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Generated;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/device")
@Slf4j
@Authorize
@Resource(id = "device-instance", name = "设备实例")
@Tag(name = "设备指令API")
@Deprecated
@Generated
public class DeviceMessageController {

    @Autowired
    public LocalDeviceInstanceService instanceService;

    //获取设备属性
    @GetMapping("/{deviceId}/property/{property:.+}")
    @SneakyThrows
    @QueryAction
    @Deprecated
    @Generated
    public Flux<?> getProperty(@PathVariable String deviceId, @PathVariable String property) {
        return instanceService
            .readProperty(deviceId, property)
            .flux();

    }

    //获取标准设备属性
    @GetMapping("/standard/{deviceId}/property/{property:.+}")
    @SneakyThrows
    @QueryAction
    @Deprecated
    @Generated
    public Mono<DeviceProperty> getStandardProperty(@PathVariable String deviceId, @PathVariable String property) {
        return instanceService.readAndConvertProperty(deviceId, property);

    }

    //设置设备属性
    @PostMapping("/setting/{deviceId}/property")
    @SneakyThrows
    @QueryAction
    @Deprecated
    @Generated
    public Flux<?> writeProperties(@PathVariable String deviceId, @RequestBody Mono<Map<String, Object>> properties) {
        return properties.flatMapMany(props -> instanceService.writeProperties(deviceId, props));
    }

    //设备功能调用
    @PostMapping("/invoked/{deviceId}/function/{functionId}")
    @SneakyThrows
    @QueryAction
    @Deprecated
    @Generated
    public Flux<?> invokedFunction(@PathVariable String deviceId,
                                   @PathVariable String functionId,
                                   @RequestBody Mono<Map<String, Object>> properties) {

        return properties.flatMapMany(props -> instanceService.invokeFunction(deviceId, functionId, props));


    }

    //获取设备所有属性
    @PostMapping("/{deviceId}/properties")
    @SneakyThrows
    @QueryAction
    @Deprecated
    @Generated
    public Flux<?> getProperties(@PathVariable String deviceId,
                                 @RequestBody Flux<String> properties) {

        return properties.collectList().flatMapMany(list -> instanceService.readProperties(deviceId, list));
    }

}
