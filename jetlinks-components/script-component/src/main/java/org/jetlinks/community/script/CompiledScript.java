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

import java.util.Collections;
import java.util.Map;

/**
 * 已编译的脚本信息.
 *
 * @author zhouhao
 * @since 2.0
 */
public interface CompiledScript {

    /**
     * 使用指定上下文执行脚本
     *
     * @param context 上下文
     * @return 脚本返回结果
     */
    Object call(ExecutionContext context);

    default Object call(Map<String, Object> context) {
        return call(ExecutionContext.create(context));
    }

    default Object call() {
        return call(Collections.emptyMap());
    }

}
