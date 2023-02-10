package org.jetlinks.community.device.events;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.event.DefaultAsyncEvent;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@Generated
public class DeviceUnregisterEvent extends DefaultAsyncEvent {

    private List<DeviceInstanceEntity> devices;

}
