/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.rule.engine.scene;

import org.jetlinks.community.rule.engine.alarm.AlarmConstants;
import org.jetlinks.community.terms.TermSpec;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 场景动作提供商,用于提供场景动作的配置,变量,规则节点等信息
 *
 * @param <C> 配置类型
 * @author zhouhao
 * @since 2.1
 */
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
     * 判断当前服务是否支持此触发器
     *
     * @return 是否支持
     * @since 2.3
     */
    default Mono<Boolean> isSupported() {
        return Reactors.ALWAYS_TRUE;
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
