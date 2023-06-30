package org.jetlinks.community.device.web.excel;

import lombok.Getter;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.device.entity.DeviceProductEntity;
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
 * @author zhangji 2023/6/28
 * @since 2.1
 */
public class DeviceExcelImporter extends AbstractImporter<DeviceExcelInfo> {

    @Getter
    private final DeviceProductEntity product;

    private final Map<String, PropertyMetadata> tagMapping = new HashMap<>();

    private final Map<String, ConfigPropertyMetadata> configMapping = new HashMap<>();

    private final Authentication auth;

    public DeviceExcelImporter(FileManager fileManager,
                               WebClient client,
                               DeviceProductEntity product,
                               List<ConfigPropertyMetadata> configs,
                               Authentication auth) {
        super(fileManager, client);
        this.product = product;
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
            .map(deviceExcelInfo -> deviceExcelInfo.initDeviceInstance(product, auth))
            .then();
    }

    @Override
    protected DeviceExcelInfo newInstance() {
        DeviceExcelInfo deviceExcelInfo = new DeviceExcelInfo();
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
