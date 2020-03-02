package org.jetlinks.community.device.enums;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.core.message.DeviceMessage;

@AllArgsConstructor
@Getter
public enum DeviceLogType implements EnumDict<String> {
    event("事件上报"),
    readProperty("属性读取"),
    writeProperty("属性修改"),
    reportProperty("属性上报"),
    child("子设备消息"),
    functionInvoke("调用功能"),
    readPropertyReply("读取属性回复"),
    writePropertyReply("修改属性回复"),
    functionReply("调用功能回复"),

    offline("离线"),
    online("上线"),
    other("其它");

    @JSONField(serialize = false)
    private String text;

    @Override
    public String getValue() {
        return name();
    }


    public static DeviceLogType of(DeviceMessage message) {
        switch (message.getMessageType()) {
            case EVENT:
                return event;
            case ONLINE:
                return online;
            case OFFLINE:
                return offline;
            case CHILD:
                return child;
            case REPORT_PROPERTY:
                return reportProperty;
            case INVOKE_FUNCTION_REPLY:
                return functionReply;
            case READ_PROPERTY_REPLY:
                return readPropertyReply;
            case WRITE_PROPERTY_REPLY:
                return writePropertyReply;
            default:
                return other;
        }

    }


//    @Override
//    public Object getWriteJSONObject() {
//        return getValue();
//    }
}
