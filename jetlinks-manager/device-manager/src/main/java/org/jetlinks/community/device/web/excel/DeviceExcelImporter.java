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
package org.jetlinks.community.device.web.excel;

import lombok.Getter;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.io.excel.AbstractImporter;
import org.jetlinks.community.io.excel.ImportHelper;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.core.metadata.ConfigPropertyMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备导入.
 *
 * @author zhangji 2023/6/25
 * @since 2.1
 */
public class DeviceExcelImporter extends AbstractImporter<DeviceExcelInfo> {

    @Getter
    private final DeviceProductEntity product;

    private final Map<String, PropertyMetadata> tagMapping = new HashMap<>();

    private final Map<String, ConfigPropertyMetadata> configMapping = new HashMap<>();

    private final Authentication auth;

    private final LocalDeviceInstanceService deviceService;

    public DeviceExcelImporter(FileManager fileManager,
                               WebClient client,
                               DeviceProductEntity product,
                               List<ConfigPropertyMetadata> configs,
                               LocalDeviceInstanceService deviceService,
                               Authentication auth) {
        super(fileManager, client);
        this.product = product;
        this.deviceService = deviceService;
        this.auth = auth;
        List<PropertyMetadata> tags = product.parseMetadata().getTags();
        for (PropertyMetadata tag : tags) {
            tagMapping.put(tag.getName(), tag);
        }
        for (ConfigPropertyMetadata config : configs) {
            configMapping.put(config.getName(), config);
        }
    }

    @Override
    protected Mono<Void> handleData(Flux<DeviceExcelInfo> data) {
        return data
            .doOnNext(ValidatorUtils::tryValidate)
            .map(deviceExcelInfo -> deviceExcelInfo.initDeviceInstance(product,auth))
            .then();
    }

    @Override
    protected DeviceExcelInfo newInstance() {
        DeviceExcelInfo deviceExcelInfo = EntityFactoryHolder.newInstance(DeviceExcelInfo.class,DeviceExcelInfo::new);
        deviceExcelInfo.setTagMapping(tagMapping);
        deviceExcelInfo.setConfigMapping(configMapping);
        return deviceExcelInfo;
    }

    @Override
    protected void customImport(ImportHelper<DeviceExcelInfo> helper) {
        helper.fallbackSingle(true);
        for (PropertyMetadata tag : tagMapping.values()) {
            helper.addHeader(tag.getId(), tag.getName());
        }
        for (ConfigPropertyMetadata config : configMapping.values()) {
            helper.addHeader(config.getProperty(), config.getName());
        }
    }
}
