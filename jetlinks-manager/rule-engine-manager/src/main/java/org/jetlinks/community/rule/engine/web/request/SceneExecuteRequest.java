package org.jetlinks.community.rule.engine.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 触发场景请求.
 *
 * @author zhangji 2022/12/14
 */
@Getter
@Setter
public class SceneExecuteRequest {

    @Schema(description = "场景ID")
    private String id;

    @Schema(description = "数据")
    private Map<String, Object> context;

}
