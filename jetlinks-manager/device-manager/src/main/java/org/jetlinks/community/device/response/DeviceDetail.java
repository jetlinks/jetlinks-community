package org.jetlinks.community.device.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.device.enums.DeviceFeature;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.enums.DeviceType;
import org.jetlinks.community.relation.service.response.RelatedInfo;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.Values;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.metadata.*;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
public class DeviceDetail {

    //设备ID
    @Schema(description = "设备ID")
    private String id;

    //设备名称
    @Schema(description = "设备名称")
    private String name;

    //设备图片
    @Schema(description = "图片地址")
    private String photoUrl;

    //消息协议标识
    @Schema(description = "消息协议ID")
    private String protocol;

    //协议名称
    @Schema(description = "消息协议名称")
    private String protocolName;

    //通信协议
    @Schema(description = "通信协议")
    private String transport;

    //所属机构ID
    @Schema(description = "机构ID")
    private String orgId;

    //所属机构名称
    @Schema(description = "机构名称")
    private String orgName;

    //产品ID
    @Schema(description = "产品ID")
    private String productId;

    //型号名称
    @Schema(description = "产品名称")
    private String productName;

    //设备类型
    @Schema(description = "设备类型")
    private DeviceType deviceType;

    //设备状态
    @Schema(description = "设备状态")
    private DeviceState state;

    //客户端地址 /id:port
    @Schema(description = "ip地址")
    private String address;

    //上线时间
    @Schema(description = "上线时间")
    private long onlineTime;

    //离线时间
    @Schema(description = "离线时间")
    private long offlineTime;

    //创建时间
    @Schema(description = "创建时间")
    private long createTime;

    //激活时间
    @Schema(description = "激活时间")
    private long registerTime;

    //设备元数据
    @Schema(description = "物模型")
    private String metadata;

    //是否为独立物模型
    @Schema(description = "是否为独立物模型")
    private boolean independentMetadata;

    //设备配置信息
    @Schema(description = "配置信息")
    private Map<String, Object> configuration = new HashMap<>();

    //已生效的配置信息
    @Schema(description = "已生效的配置信息")
    private Map<String, Object> cachedConfiguration = new HashMap<>();

    //设备单独的配置信息
    @Schema(description = "是否为单独的配置,false表示部分配置信息继承自产品.")
    private boolean aloneConfiguration;

    //父设备ID
    @Schema(description = "父设备ID")
    private String parentId;

    //标签
    @Schema(description = "标签信息")
    private List<DeviceTagEntity> tags = new ArrayList<>();

    @Schema(description = "设备描述")
    private String description;

    @Schema(description = "关系信息")
    private List<RelatedInfo> relations;

    @Schema(description = "设备特性")
    private List<Feature> features = new ArrayList<>();


    @Schema(description = "设备接入方式ID")
    private String accessId;

    @Schema(description = "设备接入方式")
    private String accessProvider;

    @Schema(description = "设备接入方式名称")
    private String accessName;

    @Schema(description = "产品所属品类ID")
    private String classifiedId;

    @Schema(description = "产品所属品类名称")
    private String classifiedName;



    public DeviceDetail notActive() {

        state = DeviceState.notActive;
        initTags();
        return this;
    }

    private DeviceMetadata decodeMetadata() {
        if (StringUtils.isEmpty(metadata)) {
            return null;
        }
        return JetLinksDeviceMetadataCodec.getInstance().doDecode(metadata);
    }

    private void initTags() {
        DeviceMetadata metadata = decodeMetadata();
        if (null != metadata) {
            with(metadata
                     .getTags()
                     .stream()
                     .map(DeviceTagEntity::of)
                     .collect(Collectors.toList()));
        }
    }


    public Mono<DeviceDetail> with(DeviceOperator operator, List<ConfigPropertyMetadata> configs) {
        return Mono
            .zip(
                //T1: 远程地址
                operator.getAddress().defaultIfEmpty("/"),
                //T2: 上线时间
                operator.getOnlineTime().defaultIfEmpty(0L),
                //T3: 离线时间
                operator.getOfflineTime().defaultIfEmpty(0L),
                //T4: 物模型
                operator.getMetadata().switchIfEmpty(Mono.fromSupplier(this::decodeMetadata)),
                //T5: 真实的配置信息
                operator.getSelfConfigs(configs
                                            .stream()
                                            .map(ConfigPropertyMetadata::getProperty)
                                            .collect(Collectors.toList()))
                         .defaultIfEmpty(Values.of(Collections.emptyMap()))
            )
            .doOnNext(tp -> {
                setOnlineTime(tp.getT2());
                setOfflineTime(tp.getT3());
                setAddress(tp.getT1());
                with(tp.getT4()
                       .getTags()
                       .stream()
                       .map(DeviceTagEntity::of)
                       .collect(Collectors.toList()));
                Map<String, Object> cachedConfigs = tp.getT5().getAllValues();
                cachedConfiguration.putAll(cachedConfigs);
//                cachedConfigs.forEach(configuration::putIfAbsent);
            })
            .thenReturn(this);
    }

    public synchronized DeviceDetail with(List<DeviceTagEntity> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return this;
        }
        Map<String, DeviceTagEntity> map = Stream
            .concat(tags.stream(), this.tags.stream())
            .collect(
                Collectors.toMap(
                    DeviceTagEntity::getKey,
                    Function.identity(),
                    (_1, _2) -> StringUtils.hasText(_1.getValue()) ? _1 : _2));

        this.tags = new ArrayList<>(map.values());

        DeviceMetadata deviceMetadata = decodeMetadata();
        if (null != deviceMetadata) {
            this.tags.sort(Comparator
                               .comparingLong(tag -> {
                                   PropertyMetadata tagMetadata = deviceMetadata.getTagOrNull(tag.getKey());
                                   return tagMetadata == null
                                       ? tag.getCreateTime().getTime()
                                       : deviceMetadata.getTags().indexOf(tagMetadata);
                               }));
        } else {
            this.tags.sort(Comparator.comparing(DeviceTagEntity::getCreateTime));
        }

        if (StringUtils.hasText(id)) {
            for (DeviceTagEntity tag : getTags()) {
                tag.setId(DeviceTagEntity.createTagId(id, tag.getKey()));
            }
        }
        return this;
    }

    public DeviceDetail withRelation(List<RelatedInfo> relations){
        this.relations=relations;
        return this;
    }

    public DeviceDetail with(DeviceProductEntity productEntity) {
        if (productEntity == null) {
            return this;
        }
        if (StringUtils.isEmpty(metadata)) {
            setMetadata(productEntity.getMetadata());
        }
        if (CollectionUtils.isEmpty(configuration) && !CollectionUtils.isEmpty(productEntity.getConfiguration())) {
            configuration.putAll(productEntity.getConfiguration());
        }
        setProtocol(productEntity.getMessageProtocol());
        setTransport(productEntity.getTransportProtocol());
        setPhotoUrl(productEntity.getPhotoUrl());
        setProductId(productEntity.getId());
        setProductName(productEntity.getName());
        setDeviceType(productEntity.getDeviceType());
        setProtocolName(productEntity.getProtocolName());
        setAccessProvider(productEntity.getAccessProvider());
        setAccessId(productEntity.getAccessId());
        setAccessName(productEntity.getAccessName());
        setClassifiedId(productEntity.getClassifiedId());
        setClassifiedName(productEntity.getClassifiedName());
        return this;
    }

    public DeviceDetail with(DeviceInstanceEntity device) {

        setId(device.getId());
        setName(device.getName());
        setState(device.getState());
        setOrgId(device.getOrgId());
        setParentId(device.getParentId());
        setDescription(device.getDescribe());
        if (device.getFeatures() != null) {
            withFeatures(Arrays.asList(device.getFeatures()));
        }
        Optional.ofNullable(device.getRegistryTime())
                .ifPresent(this::setRegisterTime);

        Optional.ofNullable(device.getCreateTime())
                .ifPresent(this::setCreateTime);

        if (MapUtils.isNotEmpty(device.getConfiguration())) {
            boolean hasConfig = device
                .getConfiguration()
                .keySet()
                .stream()
                .map(configuration::get)
                .anyMatch(Objects::nonNull);
            if (hasConfig) {
                setAloneConfiguration(true);
            }
            configuration.putAll(device.getConfiguration());
        }
        if (StringUtils.hasText(device.getDeriveMetadata())) {
            setMetadata(device.getDeriveMetadata());
            setIndependentMetadata(true);
        }

        for (DeviceTagEntity tag : getTags()) {
            tag.setId(DeviceTagEntity.createTagId(id, tag.getKey()));
        }

        return this;
    }

    public DeviceDetail withFeatures(Collection<? extends Feature> features) {
        for (Feature feature : features) {
            this.features.add(new SimpleFeature(feature.getId(), feature.getName()));
        }
        return this;
    }

    public Mono<DeviceDetail> with(DeviceProductOperator product) {
        return Mono
            .zip(
                product
                    .getProtocol()
                    .mapNotNull(ProtocolSupport::getName)
                    .defaultIfEmpty(""),
                product
                    .getConfig(DeviceConfigKey.metadata)
                    .defaultIfEmpty(""))
            .doOnNext(tp2 -> {
                setProtocolName(tp2.getT1());
                //物模型以产品缓存里的为准
                if (!this.independentMetadata && StringUtils.hasText(tp2.getT2())) {
                    setMetadata(tp2.getT2());
                }
            })
            .thenReturn(this);
    }


}
