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
package org.jetlinks.community.script.context;

import javax.script.ScriptContext;
import java.util.Map;
import java.util.function.Function;

public interface ExecutionContext extends ScriptContext {

    boolean hasAttribute(String key);

    @SafeVarargs
    static ExecutionContext create(Map<String, Object>... context) {
        return new DefaultExecutionContext(context);
    }

    @SafeVarargs
    static ExecutionContext create(Function<String, Object> fallback, Map<String, Object>... context) {
        return new DefaultExecutionContext(context, fallback);
    }

    static ExecutionContext compose(ExecutionContext... contexts) {
        return new CompositeExecutionContext(contexts);
    }

    default ExecutionContext merge(ExecutionContext target) {
        return compose(this, target);
    }


}
