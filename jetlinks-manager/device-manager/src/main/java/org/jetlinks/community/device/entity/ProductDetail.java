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
package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.Feature;
import org.jetlinks.core.metadata.SimpleFeature;
import org.jetlinks.community.device.enums.DeviceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 产品详情信息.
 *
 * @author zhangji 2022/7/28
 */
@Getter
@Setter
public class ProductDetail {
    @Schema(description = "产品ID")
    private String id;

    @Schema(description = "产品名称")
    private String name;

    @Schema(description = "所属项目")
    private String projectId;

    @Schema(description = "图片地址")
    private String photoUrl;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "说明")
    private String describe;

    @Schema(description = "所属品类ID")
    private String classifiedId;

    @Schema(description = "所属品类名称")
    private String classifiedName;

    @Schema(description = "消息协议ID")
    private String messageProtocol;

    @Schema(description = "消息协议名称")
    private String protocolName;

    @Schema(description = "物模型定义")
    private String metadata;

    @Schema(description = "传输协议")
    private String transportProtocol;

    @Schema(description = "入网方式")
    private String networkWay;

    @Schema(description = "设备类型")
    private DeviceType deviceType;

    @Schema(description = "设备数量")
    private int deviceCount;

    @Schema(description = "协议相关配置")
    private Map<String, Object> configuration;

    @Schema(description = "产品状态 1正常,0禁用")
    private Byte state;

    @Schema(description = "创建者ID(只读)")
    private String creatorId;

    @Schema(description = "创建者时间(只读)")
    private Long createTime;

    @Schema(description = "设备接入方式ID")
    private String accessId;

    @Schema(description = "设备接入方式")
    private String accessProvider;

    @Schema(description = "设备接入方式名称")
    private String accessName;

    @Schema(description = "数据存储策略")
    private String storePolicy;

    @Schema(description = "数据存储策略相关配置")
    private Map<String, Object> storePolicyConfiguration;

    @Schema(description = "产品配置信息")
    private List<ConfigMetadata> configMetadatas = new ArrayList<>();

    @Schema(description = "产品特性")
    private List<Feature> features = new ArrayList<>();

    public static ProductDetail of(){
        return EntityFactoryHolder.newInstance(ProductDetail.class,ProductDetail::new);
    }

    public ProductDetail with(DeviceProductEntity entity) {
        FastBeanCopier.copy(entity, this);
        return this;
    }

    public ProductDetail withConfigMetadatas(Collection<? extends ConfigMetadata> configMetadatas) {
        this.configMetadatas.addAll(configMetadatas);
        return this;
    }

    public ProductDetail withFeatures(Collection<? extends Feature> features) {
        for (Feature feature : features) {
            this.features.add(new SimpleFeature(feature.getId(), feature.getName()));
        }
        return this;
    }

    public ProductDetail withDeviceCount(Integer deviceCount) {
        this.setDeviceCount(deviceCount);
        return this;
    }
}



