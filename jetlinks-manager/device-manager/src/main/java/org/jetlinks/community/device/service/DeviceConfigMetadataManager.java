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
package org.jetlinks.community.device.service;

import org.hswebframework.utils.MapUtils;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.metadata.*;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.spi.DeviceConfigMetadataSupplier;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 设备配置信息管理器,用于获取产品或者设备在运行过程中所需要的配置信息。
 * <p>
 * 这些配置可以在协议包中{@link org.jetlinks.core.defaults.CompositeProtocolSupport#addConfigMetadata(Transport, ConfigMetadata)}进行定义
 * 或者通过实现接口{@link DeviceConfigMetadataSupplier}来定义
 * <p>
 * 在定义配置时,可以通过指定{@link ConfigPropertyMetadata#getScopes()}来定义配置的作用域返回。
 * <p>
 * 比如:
 * <pre>
 *
 *  new DefaultConfigMetadata()
 *      .add("apiUrl","API地址",StringType.GLOBAL,DeviceConfigScope.product) //只作用于产品配置
 *      .add("password","密码",StringType.GLOBAL,DeviceConfigScope.device); //只作用于设备配置
 *
 * </pre>
 * <p>
 * 注意：所有的配置都是保存在一起的，在定义字段时，要注意配置名冲突。
 *
 * @author zhouhao
 * @see DeviceConfigMetadataSupplier
 * @see DeviceInstanceEntity#getConfiguration()
 * @see DeviceProductEntity#getConfiguration()
 * @see org.jetlinks.core.device.DeviceOperator#getConfig(String)
 * @since 1.6
 */
public interface DeviceConfigMetadataManager {

    /**
     * 根据设备ID获取配置信息
     *
     * @param deviceId 产品ID
     * @return 配置信息
     * @see DeviceConfigScope#device
     */
    Flux<ConfigMetadata> getDeviceConfigMetadata(String deviceId);

    /**
     * 根据产品ID获取设备需要的配置定义信息
     *
     * @param productId 产品ID
     * @return 配置信息
     */
    Flux<ConfigMetadata> getDeviceConfigMetadataByProductId(String productId);

    /**
     * 根据产品ID获取产品所需配置信息
     *
     * @param productId 产品ID
     * @return 配置信息
     */
    Flux<ConfigMetadata> getProductConfigMetadata(String productId);

    /**
     * 根据产品ID和网关ID获取配置信息
     * <p>
     * 使用指定的接入方式查询，忽略产品当前绑定的接入方式
     * <p>
     * 当配置来自产品绑定关系时，可根据productId查询
     * <p>
     * 当配置来自接入方式时，可根据accessId查询
     * <p>
     * 当配置来自协议包时，可根据accessId关联的协议查询
     *
     * @param productId 产品ID
     * @param accessId 网关ID
     * @return 配置信息
     */
    Flux<ConfigMetadata> getProductConfigMetadataByAccessId(String productId,
                                                            String accessId);

    /**
     * 获取物模型拓展配置定义
     * @param productId 产品ID
     * @param metadataType 物模型类型
     * @param metadataId 物模型ID
     * @param typeId 类型
     * @return 配置定义信息
     */
    Flux<ConfigMetadata> getMetadataExpandsConfig(String productId,
                                                  DeviceMetadataType metadataType,
                                                  String metadataId,
                                                  String typeId,
                                                  ConfigScope... scopes);

    Flux<Feature> getProductFeatures(String productId);

    /**
     * 检验配置中的属性必填项
     * @param configMetadata 产品/设备所需配置信息
     * @param configuration 产品/设备配置信息
     * @return 验证结果
     */
    static Mono<ValidateResult> validate(ConfigMetadata configMetadata,
                                         Map<String, Object> configuration) {
        // 必填项配置为空，不进行校验
        if (configMetadata == null || configMetadata.getProperties() == null) {
            return Mono.just(ValidateResult.success());
        }
        return Flux
            .fromIterable(configMetadata.getProperties())
            .filter(propertyMetadata -> {
                // 过滤出必填项
                DataType dataType = propertyMetadata.getType();
                if (dataType == null) {
                    return false;
                }
                return dataType
                    .getExpand(ConfigMetadataConstants.required.getKey())
                    .map(CastUtils::castBoolean)
                    .orElse(false);
            })
            .map(ConfigPropertyMetadata::getProperty)
            // 配置为空或不包含必填项，都不通过校验
            .filter(property -> MapUtils.isNullOrEmpty(configuration) || configuration.get(property) == null)
            .collectList()
            .filter(property -> !CollectionUtils.isEmpty(property))
            .map(property -> ValidateResult
                .builder()
                .errorMsg(LocaleUtils.resolveMessage("error.config_metadata_required_must_not_be_null", property))
                .value(property)
                .build());
    }
}
