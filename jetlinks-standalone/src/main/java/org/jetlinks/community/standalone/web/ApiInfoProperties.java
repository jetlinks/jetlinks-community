package org.jetlinks.community.standalone.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@ConfigurationProperties(prefix = "api")
@Component
public class ApiInfoProperties {

    @Schema(description = "api根路径")
    private String basePath;

    @Schema(description = "api地址信息")
    private Map<String, String> urls = new ConcurrentHashMap<>();

}
