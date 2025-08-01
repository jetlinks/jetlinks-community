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
package org.jetlinks.community.script;

import org.jetlinks.community.script.context.ExecutionContext;

import java.util.Map;

/**
 * 提供支持暴露方法的脚本
 *
 * @param <T> 暴露方法的实例类型
 * @author zhouhao
 * @since 2.0
 */
public interface ExposedScript<T> {

    /**
     * 使用指定的暴露实例和上下文来执行脚本.在脚本中访问暴露的方法将调用指定实例的指定方法.
     *
     * @param expose  需要暴露的实例
     * @param context 上下文
     * @return 脚本执行结果
     */
    Object call(T expose, ExecutionContext context);

    default Object call(T expose, Map<String, Object> context) {
        return call(expose, ExecutionContext.create(context));
    }

    default Object call(T expose) {
        return call(expose, ExecutionContext.create());
    }

}
