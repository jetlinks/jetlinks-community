package org.jetlinks.community.resource.initialize;

import com.alibaba.fastjson.JSON;
import org.hswebframework.web.authorization.define.AuthorizeDefinitionInitializedEvent;
import org.hswebframework.web.authorization.define.MergedAuthorizeDefinition;
import org.jetlinks.community.resource.Resource;
import org.jetlinks.community.resource.ResourceProvider;
import org.jetlinks.community.resource.SimpleResource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;

public class PermissionResourceProvider implements ResourceProvider {
    public static final String type = "permission";

    private final MergedAuthorizeDefinition definition = new MergedAuthorizeDefinition();

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Flux<Resource> getResources() {
        return Flux
            .fromIterable(definition.getResources())
            .map(def -> SimpleResource.of(def.getId(), getType(), JSON.toJSONString(def)));
    }

    @Override
    public Flux<Resource> getResources(Collection<String> id) {
        return getResources()
            .filter(resource -> id == null || id.contains(resource.getId()));
    }

    @EventListener
    public void handleInitializedEvent(AuthorizeDefinitionInitializedEvent event) {
        definition.merge(event.getAllDefinition());
    }
}
