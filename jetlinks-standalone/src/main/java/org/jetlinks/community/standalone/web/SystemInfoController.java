package org.jetlinks.community.standalone.web;

import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("/system")
@RestController
public class SystemInfoController {

    private static final Map<String, Object> versionResponse;

    private final ApiInfoProperties infoProperties;

    static {
        versionResponse = new HashMap<>();
        versionResponse.put("result", new Version());
        versionResponse.put("status", 200);
        versionResponse.put("code", "success");
    }

    public SystemInfoController(ApiInfoProperties infoProperties) {
        this.infoProperties = infoProperties;
    }

    @GetMapping("/version")
    public Mono<Map<String, Object>> getVersion() {

        return Mono.just(versionResponse);
    }

    @GetMapping("/apis")
    public Mono<Map<String, Object>> getApis() {
        Map<String, Object> map = new HashMap<>();
        map.put("result", infoProperties);
        map.put("status", 200);
        map.put("code", "success");
        return Mono.just(map);
    }


    @Getter
    public static class Version {
        private final String edition = "pro";
        private final String version = "1.1.0-SNAPSHOT";
        private final String mode = "cloud";

    }
}
