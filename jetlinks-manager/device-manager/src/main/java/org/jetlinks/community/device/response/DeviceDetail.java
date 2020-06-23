package org.jetlinks.community.device.response;

import lombok.Getter;
import lombok.Setter;
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
    private String id;

    //设备名称
    private String name;

    //设备图片
    private String photoUrl;

    //消息协议标识
    private String protocol;

    //协议名称
    private String protocolName;

    //通信协议
    private String transport;

    //所属机构ID
    private String orgId;

    //所属机构名称
    private String orgName;

    //型号ID
    private String productId;

    //型号名称
    private String productName;

    //设备状态
    private DeviceState state;

    //设备类型
    private DeviceType deviceType;

    //客户端地址 /id:port
    private String address;

    //上线时间
    private long onlineTime;

    //离线时间
    private long offlineTime;

    //创建时间
    private long createTime;

    //注册时间
    private long registerTime;

    //设备元数据
    private String metadata;

    //设备配置信息
    private Map<String, Object> configuration = new HashMap<>();

    //设备单独的配置信息
    private boolean aloneConfiguration;

    //标签
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
        if (StringUtils.isEmpty(metadata)) {
            setMetadata(productEntity.getMetadata());
        }
        if (CollectionUtils.isEmpty(configuration) && !CollectionUtils.isEmpty(productEntity.getConfiguration())) {
            setConfiguration(productEntity.getConfiguration());
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

        Optional.ofNullable(device.getRegistryTime())
            .ifPresent(this::setRegisterTime);

        Optional.ofNullable(device.getCreateTime())
            .ifPresent(this::setCreateTime);

        if (!CollectionUtils.isEmpty(device.getConfiguration())) {
            setConfiguration(device.getConfiguration());
            setAloneConfiguration(true);
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
