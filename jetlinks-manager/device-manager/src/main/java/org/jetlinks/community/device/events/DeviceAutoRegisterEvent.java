package org.jetlinks.community.device.events;

import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.event.DefaultAsyncEvent;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;

@Getter
@Generated
public class DeviceAutoRegisterEvent extends DefaultAsyncEvent {

    private boolean allowRegister = true;

    private final DeviceInstanceEntity entity;

    public DeviceAutoRegisterEvent(DeviceInstanceEntity entity) {
        this.entity = entity;
    }

    /**
     * 设置是否允许自定注册此设备
     *
     * @param allowRegister 是否允许自动注册
     */
    @Generated
    public void setAllowRegister(boolean allowRegister) {
        this.allowRegister = allowRegister;
    }
}
