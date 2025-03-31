package org.jetlinks.community.rule.engine.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.rule.engine.scene.SceneTriggerProvider;

/**
 * 触发器类型.
 *
 * @author zhangji 2025/1/8
 * @since 2.3
 */
@Getter
@Setter
public class SceneTriggerInfo {

    @Schema(description = "类型")
    private String provider;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    public String getName() {
        return LocaleUtils.resolveMessage("message.scene_trigger_name_" + provider, name);
    }

    public String getDescription() {
        return LocaleUtils.resolveMessage("message.scene_trigger_desc_" + provider, description);
    }

    public static SceneTriggerInfo of(SceneTriggerProvider<?> triggerProvider) {
        String provider = triggerProvider.getProvider();
        SceneTriggerInfo triggerInfo = new SceneTriggerInfo();
        triggerInfo.setProvider(provider);
        triggerInfo.setName(triggerProvider.getName());
        return triggerInfo;
    }

}