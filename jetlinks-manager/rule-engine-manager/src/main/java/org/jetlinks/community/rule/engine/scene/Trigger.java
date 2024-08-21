package org.jetlinks.community.rule.engine.scene;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.EmptySqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.EmptySqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.jetlinks.community.rule.engine.commons.ShakeLimit;
import org.jetlinks.community.rule.engine.scene.internal.triggers.*;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.rule.engine.scene.term.limit.ShakeLimitGrouping;
import org.jetlinks.community.terms.TermSpec;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Getter
@Setter
public class Trigger implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "触发方式")
    @NotNull(message = "error.scene_rule_trigger_cannot_be_null")
    private String type;

    /**
     * @deprecated {@link SceneConditionAction#getShakeLimit()}
     */
    @Deprecated
    @Schema(description = "防抖配置")
    private GroupShakeLimit shakeLimit;

    @Schema(description = "[type]为[device]时不能为空")
    private DeviceTrigger device;

    @Schema(description = "[type]为[timer]时不能为空")
    private TimerTrigger timer;

    @Schema(description = "[type]不为[device,timer,collector]时不能为空")
    private Map<String, Object> configuration;


    public String getTypeName(){
        return provider().getName();
    }
    /**
     * 重构查询条件,替换为实际将要输出的变量.
     *
     * @param terms 条件
     * @return 重构后的条件
     * @see SceneTriggerProvider#refactorTerm(String, Term)
     */
    public List<Term> refactorTerm(String tableName, List<Term> terms) {
        if (CollectionUtils.isEmpty(terms)) {
            return terms;
        }
        List<Term> target = new ArrayList<>(terms.size());
        for (Term term : terms) {
            Term copy = term.clone();
            target.add(provider().refactorTerm(tableName, copy));
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(copy.getTerms())) {
                copy.setTerms(refactorTerm(tableName, copy.getTerms()));
            }
        }
        return target;
    }

    public Term refactorTerm(String tableName, Term term) {
        return provider().refactorTerm(tableName, term);
    }

    public SqlRequest createSql(List<Term> terms, boolean hasWhere) {
        SceneTriggerProvider.TriggerConfig config = triggerConfig();

        return config == null ? EmptySqlRequest.INSTANCE : provider().createSql(config, terms, hasWhere);
    }

    public SqlFragments createFilter(List<Term> terms) {
        SceneTriggerProvider.TriggerConfig config = triggerConfig();

        return config == null ? EmptySqlFragments.INSTANCE : provider().createFilter(config, terms);
    }

    public Mono<List<TermSpec>> createFilterSpec(List<Term> terms, BiConsumer<Term,TermSpec> customizer){
        return provider().createFilterSpec(triggerConfig(), terms,customizer);
    }

    public Flux<TermColumn> parseTermColumns() {
        SceneTriggerProvider.TriggerConfig config = triggerConfig();

        return config == null ? Flux.empty() : provider().parseTermColumns(config);
    }

    public SceneTriggerProvider.TriggerConfig triggerConfig() {
        switch (type) {
            case DeviceTriggerProvider.PROVIDER:
                return device;
            case TimerTriggerProvider.PROVIDER:
                return timer;
            default:
                SceneTriggerProvider.TriggerConfig config = provider().newConfig();
                if (configuration != null) {
                    config.with(configuration);
                }
                return config;
        }
    }

    SceneTriggerProvider<SceneTriggerProvider.TriggerConfig> provider() {
        return SceneProviders.getTriggerProviderNow(type);
    }

    public void validate() {
        Assert.notNull(type, "error.scene_rule_trigger_cannot_be_null");
        triggerConfig().validate();
    }

    public List<Variable> createDefaultVariable() {
        SceneTriggerProvider.TriggerConfig config = triggerConfig();

        return config == null ? Collections.emptyList() : provider().createDefaultVariable(config);
    }

    public static Trigger device(DeviceTrigger device) {
        Trigger trigger = new Trigger();
        trigger.setType(DeviceTriggerProvider.PROVIDER);
        trigger.setDevice(device);
        return trigger;
    }

    public static Trigger manual() {
        Trigger trigger = new Trigger();
        trigger.setType(ManualTriggerProvider.PROVIDER);
        return trigger;
    }

    @Getter
    @Setter
    public static class GroupShakeLimit extends ShakeLimit {

        //暂时没有实现其他方式
        @Schema(description = "分组类型:device,product,org...")
        @Hidden
        private String groupType;

        public ShakeLimitGrouping<Map<String, Object>> createGrouping() {
            //todo 其他分组方式实现
            return flux -> flux
                .groupBy(map -> map.getOrDefault("deviceId", "null"), Integer.MAX_VALUE);
        }
    }

    void applyModel(RuleModel model, RuleNodeModel sceneNode) {
        provider().applyRuleNode(triggerConfig(), model, sceneNode);
    }

}
