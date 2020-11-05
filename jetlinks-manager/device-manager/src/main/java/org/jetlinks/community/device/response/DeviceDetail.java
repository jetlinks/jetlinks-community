package org.jetlinks.community.device.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.jetlinks.community.device.enums.DeviceType;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.device.enums.DeviceState;
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

    //设备配置信息
    @Schema(description = "配置信息")
    private Map<String, Object> configuration = new HashMap<>();

    //设备单独的配置信息
    @Schema(description = "是否为单独的配置,false表示部分配置信息继承自产品.")
    private boolean aloneConfiguration;

    //父设备ID
    @Schema(description = "父设备ID")
    private String parentId;

    //标签
    @Schema(description = "标签信息")
    private List<DeviceTagEntity> tags = new ArrayList<>();

    public DeviceDetail notActive() {

        state = DeviceState.notActive;
        return this;
    }

    public Mono<DeviceDetail> with(DeviceOperator operator) {
        return Mono.zip(
            operator.getAddress().defaultIfEmpty("/"),
            operator.getOnlineTime().defaultIfEmpty(0L),
            operator.getOfflineTime().defaultIfEmpty(0L),
            operator.getMetadata()
        ).doOnNext(tp -> {
            setOnlineTime(tp.getT2());
            setOfflineTime(tp.getT3());
            setAddress(tp.getT1());
            with(tp.getT4()
                   .getTags()
                   .stream()
                   .map(DeviceTagEntity::of)
                   .collect(Collectors.toList()));
        }).thenReturn(this);
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
        this.tags.sort(Comparator.comparing(DeviceTagEntity::getCreateTime));

        if (StringUtils.hasText(id)) {
            for (DeviceTagEntity tag : getTags()) {
                tag.setId(DeviceTagEntity.createTagId(id, tag.getKey()));
            }
        }
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
        return this;
    }

    public DeviceDetail with(DeviceInstanceEntity device) {

        setId(device.getId());
        setName(device.getName());
        setState(device.getState());
        setOrgId(device.getOrgId());
        setParentId(device.getParentId());
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
        }

        for (DeviceTagEntity tag : getTags()) {
            tag.setId(DeviceTagEntity.createTagId(id, tag.getKey()));
        }

        return this;
    }

}
