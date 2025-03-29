package org.jetlinks.community.rule.engine.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.rule.engine.scene.SceneActionProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 执行动作类型.
 *
 * @author zhangji 2025/1/8
 * @since 2.3
 */
@Getter
@Setter
public class SceneActionInfo {

    @Schema(description = "类型")
    private String provider;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    public String getName() {
        return LocaleUtils.resolveMessage("message.scene_action_name_" + provider, name);
    }

    public String getDescription() {
        return LocaleUtils.resolveMessage("message.scene_action_desc_" + provider, description);
    }

    public static Flux<SceneActionInfo> of(SceneActionProvider<?> actionProvider) {
        return Mono
            .justOrEmpty(actionProvider.getMode())
            .flatMapIterable(Function.identity())
            .map(SceneActionInfo::of)
            .defaultIfEmpty(SceneActionInfo.of(actionProvider.getProvider()));
    }

    public static SceneActionInfo of(String provider) {
        SceneActionInfo actionInfo = new SceneActionInfo();
        actionInfo.setProvider(provider);
        actionInfo.setName(provider);
        return actionInfo;
    }

}