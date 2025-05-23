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
package org.jetlinks.community.relation.utils;

import org.jetlinks.core.config.ConfigKey;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 动态变量工具类,对动态变量获取的支持.
 * <p>
 * 通过在上下文中设置值为{@link  VariableSource},来表示此变量值为动态变量.
 *
 * @author zhouhao
 * @see VariableSource
 * @since 2.0
 */
public class RelationUtils {

    /**
     * 从上下文中解析出值,上下文支持通过动态变量{@link VariableSource}来指定值的来源.
     *
     * <pre>{@code
     *
     * RelationUtils.resolve("phoneNumber",context,RelationConstants.UserProperty.telephone)
     *
     * }</pre>
     *
     * @param key                  key
     * @param context              上下文
     * @param relationPropertyPath 关系对象属性
     * @return 值
     */
    public static Flux<Object> resolve(String key,
                                       Map<String, Object> context,
                                       ConfigKey<String> relationPropertyPath) {
        return VariableSource.resolveValue(key, context, relationPropertyPath);

    }
}
