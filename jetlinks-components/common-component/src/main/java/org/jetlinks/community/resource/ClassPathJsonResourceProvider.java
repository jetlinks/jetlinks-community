package org.jetlinks.community.resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public abstract class ClassPathJsonResourceProvider implements ResourceProvider {

    @Getter
    private final String type;
    private final String filePath;
    private static final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    private List<Resource> cache;

    public ClassPathJsonResourceProvider(String type, String filePath) {
        this.type = type;
        this.filePath = filePath;
    }

    @Override
    public final Flux<Resource> getResources() {
        return Flux.fromIterable(cache == null ? cache = read() : cache);
    }

    @Override
    public final Flux<Resource> getResources(Collection<String> id) {
        Set<String> filter = new HashSet<>(id);
        return getResources()
            .filter(res -> filter.contains(res.getId()));
    }


    private List<Resource> read() {
        List<Resource> resources = new ArrayList<>();
        try {
            log.debug("start load {} resource [{}]", type, filePath);
            for (org.springframework.core.io.Resource resource : resourcePatternResolver.getResources(filePath)) {
                log.debug("loading {} resource {}", type, resource);
                try (InputStream inputStream = resource.getInputStream()) {
                    int index = 0;
                    for (JSONObject json : JSON.parseArray(StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8), JSONObject.class)) {
                        index++;
                        String id = getResourceId(json);
                        if (StringUtils.hasText(id)) {
                            resources.add(SimpleResource.of(id, type, json.toJSONString()));
                        } else {
                            log.warn("{} resource [{}] id (index:{}) is empty : {}", type, resource, index, json);
                        }
                    }
                } catch (Throwable err) {
                    log.debug("load {} resource {} error", type, resource, err);
                }
            }
        } catch (Throwable e) {
            log.warn("load {} resource [{}] error", type, filePath, e);
            return Collections.emptyList();
        }
        return resources;
    }

    protected String getResourceId(JSONObject data) {
        return data.getString("id");
    }
}
