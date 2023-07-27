package org.jetlinks.community.rule.engine.scene;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface SceneTriggerProvider<E extends SceneTriggerProvider.TriggerConfig> {

    String getProvider();

    String getName();

    E newConfig();

    SqlRequest createSql(E config,List<Term> terms, boolean hasWhere);

    SqlFragments createFilter(E config,List<Term> terms);

    List<Variable> createDefaultVariable(E config);

    void applyRuleNode(E config, RuleModel model, RuleNodeModel sceneNode);

    Flux<TermColumn> parseTermColumns(E config);


    interface TriggerConfig{
        void validate();

        default void with(Map<String,Object> config){
            FastBeanCopier.copy(config,this);
        }
    }
}
