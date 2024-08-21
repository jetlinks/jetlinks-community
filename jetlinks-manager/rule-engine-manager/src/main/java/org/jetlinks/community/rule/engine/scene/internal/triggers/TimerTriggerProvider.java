package org.jetlinks.community.rule.engine.scene.internal.triggers;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.EmptySqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.EmptySqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.community.reactorql.term.TermTypes;
import org.jetlinks.community.rule.engine.scene.SceneTriggerProvider;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Component
public class TimerTriggerProvider implements SceneTriggerProvider<TimerTrigger> {
    public static final String PROVIDER = "timer";

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public String getName() {
        return "定时触发";
    }

    @Override
    public TimerTrigger newConfig() {
        return new TimerTrigger();
    }

    @Override
    public SqlRequest createSql(TimerTrigger config, List<Term> terms, boolean hasFilter) {
        return EmptySqlRequest.INSTANCE;
    }

    @Override
    public SqlFragments createFilter(TimerTrigger config, List<Term> terms) {
        return EmptySqlFragments.INSTANCE;
    }

    @Override
    public List<Variable> createDefaultVariable(TimerTrigger config) {
        return Collections.singletonList(
            Variable
                .of("_now",
                    LocaleUtils.resolveMessage(
                        "message.scene_term_column_now",
                        "服务器时间"))
                .withType(DateTimeType.ID)
                .withTermType(TermTypes.lookup(DateTimeType.GLOBAL))
                .withColumn("_now")
        );
    }

    @Override
    public Flux<TermColumn> parseTermColumns(TimerTrigger config) {
        return Flux.empty();
    }

    @Override
    public void applyRuleNode(TimerTrigger config, RuleModel model, RuleNodeModel sceneNode) {
        RuleNodeModel timerNode = new RuleNodeModel();
        timerNode.setId("scene:timer");
        timerNode.setName("定时触发场景");
        timerNode.setExecutor("timer");
        //使用最小负载节点来执行定时
        timerNode.setConfiguration(FastBeanCopier.copy(config, new HashMap<>()));
        model.getNodes().add(timerNode);
        //定时->场景
        model.link(timerNode, sceneNode);
    }
}
