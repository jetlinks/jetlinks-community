package org.jetlinks.community.device.web;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.Operation;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author bestfeng
 * @since 1.9
 */
@RestController
@RequestMapping("/property-calculate-rule")
@Resource(id = "/property-calculate-rule", name = "属性计算规则")
@Authorize(ignore = true)
public class PropertyCalculateController {


    @GetMapping("/description")
    @Operation(summary = "说明")
    public Mono<Object> propertyCalculateDescription() {
        return getPropertyCalculateDescription();
    }


    @SuppressWarnings("all")
    private Mono<Object> getPropertyCalculateDescription() {
        return Mono
            .fromCallable(() -> {
                try (InputStream inputStream = new ClassPathResource("property-calculate-rule.json").getInputStream()) {
                    String calculationRule = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                    return JSON.parse(calculationRule);
                }
            });
    }
}
