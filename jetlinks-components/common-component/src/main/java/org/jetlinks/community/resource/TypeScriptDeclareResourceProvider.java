package org.jetlinks.community.resource;

import org.apache.commons.lang.StringUtils;
import org.jetlinks.core.utils.TypeScriptUtils;
import reactor.core.publisher.Flux;

import java.util.Collection;

public class TypeScriptDeclareResourceProvider implements ResourceProvider {

    public static String TYPE = "typescript-declare";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Flux<Resource> getResources() {
        return Flux.empty();
    }

    @Override
    public Flux<Resource> getResources(Collection<String> ids) {

        return Flux
            .fromIterable(ids)
            .mapNotNull(id -> {
                try {
                    String resource = TypeScriptUtils.loadDeclare(id);
                    if (StringUtils.isEmpty(resource)) {
                        return null;
                    }
                    return SimpleResource.of(
                        id,
                        getType(),
                        resource
                    );
                } catch (Throwable err) {
                    return null;
                }
            });
    }
}
