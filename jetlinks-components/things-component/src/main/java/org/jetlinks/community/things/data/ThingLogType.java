package org.jetlinks.community.things.data;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.MessageType;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
@Generated
public enum ThingLogType implements I18nEnumDict<String> {
    event("事件上报"),
    readProperty("读取属性"),
    writeProperty("修改属性"),
    writePropertyReply("修改属性回复"),
    reportProperty("属性上报"),
    readPropertyReply("读取属性回复"),
    child("子设备消息"),
    childReply("子设备消息回复"),
    functionInvoke("调用功能"),
    functionReply("调用功能回复"),
    register("设备注册"),
    unregister("设备注销"),
    readFirmware("读取固件信息"),
    readFirmwareReply("读取固件信息回复"),
    reportFirmware("上报固件信息"),
    pullFirmware("拉取固件信息"),
    pullFirmwareReply("拉取固件信息回复"),
    upgradeFirmware("推送固件信息"),
    upgradeFirmwareReply("推送固件信息回复"),
    upgradeFirmwareProgress("固件更新进度"),
    log("日志"),
    tag("标签更新"),
    offline("离线"),
    online("上线"),
    other("其它"),
    direct("透传"),
    acknowledge("应答"),
    metadata("上报物模型"),
    stateCheck("状态检查"),
    stateCheckReply("状态检查回复"),
    //状态检查
    disconnect("断开连接"),
    disconnectReply("断开连接回复");


    @JSONField(serialize = false)
    private final String text;

    @Override
    public String getValue() {
        return name();
    }

    private final static Map<MessageType, ThingLogType> typeMapping = new EnumMap<>(MessageType.class);

    public final static List<String> nameList;

    static {
        nameList = Collections.unmodifiableList(
            Arrays.stream(values())
                  .map(ThingLogType::name)
                  .collect(Collectors.toList())
        );
        typeMapping.put(MessageType.EVENT, event);
        typeMapping.put(MessageType.ONLINE, online);
        typeMapping.put(MessageType.OFFLINE, offline);
        typeMapping.put(MessageType.CHILD, child);
        typeMapping.put(MessageType.CHILD_REPLY, childReply);
        typeMapping.put(MessageType.LOG, log);
        typeMapping.put(MessageType.UPDATE_TAG, tag);

        typeMapping.put(MessageType.REPORT_PROPERTY, reportProperty);
        typeMapping.put(MessageType.READ_PROPERTY, readProperty);
        typeMapping.put(MessageType.READ_PROPERTY_REPLY, readPropertyReply);

        typeMapping.put(MessageType.INVOKE_FUNCTION, functionInvoke);
        typeMapping.put(MessageType.INVOKE_FUNCTION_REPLY, functionReply);

        typeMapping.put(MessageType.WRITE_PROPERTY, writeProperty);
        typeMapping.put(MessageType.WRITE_PROPERTY_REPLY, writePropertyReply);

        typeMapping.put(MessageType.REGISTER, register);
        typeMapping.put(MessageType.UN_REGISTER, unregister);

        typeMapping.put(MessageType.READ_FIRMWARE, readFirmware);
        typeMapping.put(MessageType.READ_FIRMWARE_REPLY, readFirmwareReply);

        typeMapping.put(MessageType.REPORT_FIRMWARE, reportFirmware);

        typeMapping.put(MessageType.REQUEST_FIRMWARE, pullFirmware);
        typeMapping.put(MessageType.REQUEST_FIRMWARE_REPLY, pullFirmwareReply);

        typeMapping.put(MessageType.UPGRADE_FIRMWARE, upgradeFirmware);
        typeMapping.put(MessageType.UPGRADE_FIRMWARE_REPLY, upgradeFirmwareReply);
        typeMapping.put(MessageType.UPGRADE_FIRMWARE_PROGRESS, upgradeFirmwareProgress);
        typeMapping.put(MessageType.ACKNOWLEDGE, acknowledge);
        typeMapping.put(MessageType.DERIVED_METADATA, metadata);
        typeMapping.put(MessageType.STATE_CHECK, stateCheck);
        typeMapping.put(MessageType.STATE_CHECK_REPLY, stateCheckReply);

        typeMapping.put(MessageType.DISCONNECT, disconnect);
        typeMapping.put(MessageType.DISCONNECT_REPLY, disconnectReply);

        typeMapping.put(MessageType.DIRECT, direct);

    }

    @Generated
    public static ThingLogType of(Message message) {
        return Optional.ofNullable(typeMapping.get(message.getMessageType())).orElse(ThingLogType.other);

    }


//    @Override
//    public Object getWriteJSONObject() {
//        return getValue();
//    }
}
