package org.jetlinks.community.rule.engine.scene.internal.triggers;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.EmptySqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.EmptySqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.community.reactorql.term.TermTypes;
import org.jetlinks.community.rule.engine.scene.AbstractSceneTriggerProvider;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "rule.scene.trigger.manual")
public class ManualTriggerProvider extends AbstractSceneTriggerProvider<ManualTrigger> {
    public static final String PROVIDER = "manual";


    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public String getName() {
        return "手动触发";
    }

    @Override
    public ManualTrigger newConfig() {
        return new ManualTrigger();
    }

    @Override
    public SqlRequest createSql(ManualTrigger config, List<Term> terms, boolean hasFilter) {
        return EmptySqlRequest.INSTANCE;
    }

    @Override
    public SqlFragments createFilter(ManualTrigger config, List<Term> terms) {
        return EmptySqlFragments.INSTANCE;
    }

    @Override
    public List<Variable> createDefaultVariable(ManualTrigger config) {
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
    public Flux<TermColumn> parseTermColumns(ManualTrigger config) {
        return Flux.empty();
    }

    @Override
    public void applyRuleNode(ManualTrigger config, RuleModel model, RuleNodeModel sceneNode) {

    }
}
