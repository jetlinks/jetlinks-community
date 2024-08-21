package org.jetlinks.community.rule.engine.scene;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.rule.engine.commons.ShakeLimit;
import org.jetlinks.community.rule.engine.commons.ShakeLimitResult;
import org.jetlinks.community.rule.engine.commons.impl.SimpleShakeLimitProvider;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.terms.TermSpec;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.jetlinks.community.rule.engine.scene.SceneRule.SOURCE_ID_KEY;

/**
 * 场景触发支持提供商,用于提供对场景触发条件的支持.
 *
 * @param <E> E 配置类型
 * @author zhouhao
 * @since 2.1
 */
public interface SceneTriggerProvider<E extends SceneTriggerProvider.TriggerConfig> {

    /**
     * @return 提供商唯一标识
     */
    String getProvider();

    /**
     * @return 名称
     */
    String getName();

    /**
     * 创建配置
     *
     * @return 配置
     */
    E newConfig();

    /**
     * 根据配置以及条件创建SQL，该sql执行基于{@link org.jetlinks.reactor.ql.ReactorQL}.
     *
     * @param config    配置
     * @param terms     条件
     * @param hasFilter 是否包含过滤条件
     * @return SQL
     * @see org.jetlinks.reactor.ql.ReactorQL
     */
    SqlRequest createSql(E config, List<Term> terms, boolean hasFilter);

    /**
     * 创建过滤条件.不含where前缀.
     *
     * @param config 配置
     * @param terms  条件
     * @return 条件SQL语句
     */
    SqlFragments createFilter(E config, List<Term> terms);

    /**
     * 重构条件为匹配场景输出变量的条件
     *
     * @param mainTableName 主表名
     * @param term          条件
     * @return 重构后的条件
     */
    default Term refactorTerm(String mainTableName,
                              Term term) {
        return SceneUtils.refactorTerm(mainTableName, term);
    }

    /**
     * 创建对用户友好的条件描述信息.
     *
     * @param config 配置信息
     * @param terms  条件
     * @return 描述信息
     * @since 2.2
     */
    default Mono<List<TermSpec>> createFilterSpec(E config, List<Term> terms, BiConsumer<Term, TermSpec> customizer) {
        return Mono.justOrEmpty(TermSpec.of(terms, customizer));
    }

    /**
     * 创建默认变量信息
     *
     * @param config 配置
     * @return 变量信息
     */
    List<Variable> createDefaultVariable(E config);

    /**
     * 应用规则节点配置
     *
     * @param config    触发器配置
     * @param model     规则模型
     * @param sceneNode 场景触发节点模型
     */
    void applyRuleNode(E config, RuleModel model, RuleNodeModel sceneNode);

    /**
     * 解析配置信息为支持的条件列,用于展示当前触发方式支持的触发条件.
     *
     * @param config 配置
     * @return 条件列信息
     */
    Flux<TermColumn> parseTermColumns(E config);

    /**
     * 配置信息
     */
    default Flux<ShakeLimitResult<Map<String, Object>>> shakeLimit(String key,
                                                                   Flux<Map<String, Object>> source,
                                                                   ShakeLimit limit) {
        return SimpleShakeLimitProvider
            .GLOBAL
            .shakeLimit(key,
                        source.groupBy(
                            data -> String.valueOf(data.getOrDefault(SOURCE_ID_KEY, "null")),
                            Integer.MAX_VALUE),
                        limit);
    }

    interface TriggerConfig {
        void validate();

        default void with(Map<String, Object> config) {
            FastBeanCopier.copy(config, this);
        }
    }
}
