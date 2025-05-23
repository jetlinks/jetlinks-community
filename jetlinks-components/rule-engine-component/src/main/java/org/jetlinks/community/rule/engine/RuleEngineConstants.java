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
package org.jetlinks.community.rule.engine;

import org.jetlinks.rule.engine.api.task.ExecutionContext;

import java.util.Optional;

public interface RuleEngineConstants {

    String ruleCreatorIdKey = "creatorId";

    String ruleName = "name";

    static Optional<String> getCreatorId(ExecutionContext context) {
        return context.getJob()
                      .getRuleConfiguration(ruleCreatorIdKey)
                      .map(String::valueOf);
    }

    static Optional<String> getRuleName(ExecutionContext context) {
        return context.getJob()
                      .getRuleConfiguration(ruleName)
                      .map(String::valueOf);
    }
}
