package org.jetlinks.community.rule.engine.scene;

import org.jetlinks.core.utils.Reactors;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.community.rule.engine.alarm.AlarmConstants;
import org.jetlinks.community.terms.TermSpec;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SceneActionProvider<C> {

    /**
     * 提供商标识
     *
     * @return 提供商标识
     */
    String getProvider();

    /**
     * 创建一个新的配置
     *
     * @return 配置
     */
    C newConfig();

    /**
     * 尝试从动作的变量中提取出需要动态获取的列信息
     *
     * @param config 配置
     * @return 名称
     */
    List<String> parseColumns(C config);

    /**
     * 根据配置创建变量,用于获取此动作将要输出的变量
     *
     * @param config 配置
     * @return 变量
     */
    Flux<Variable> createVariable(C config);

    /**
     * 应用配置到规则节点
     *
     * @param config 配置
     * @param model  规则节点
     */
    void applyRuleNode(C config, RuleNodeModel model);

    /**
     * 应用过滤条件描述到规则节点
     *
     * @param node  规则节点
     * @param specs 过滤条件
     */
    default void applyFilterSpec(RuleNodeModel node, List<TermSpec> specs) {
        node.addConfiguration(
            AlarmConstants.ConfigKey.alarmFilterTermSpecs,
            SerializeUtils.convertToSafelySerializable(specs)
        );
    }

    /**
     * 获取详细类型，
     * 用于区分同一个类型支持的多个执行动作
     * @return 详细类型
     */
    default List<String> getMode() {
        return null;
    }
}
