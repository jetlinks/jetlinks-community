package org.jetlinks.community.event;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class SystemEventHandlerRegister {

    public SystemEventHandlerRegister(ObjectProvider<SystemEventHandler> handlers){
         handlers.forEach(SystemEventHolder::register);
    }

}
