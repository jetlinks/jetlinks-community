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
package org.jetlinks.community.notify.template;

import org.jetlinks.community.notify.Provider;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.community.notify.NotifyType;
import reactor.core.publisher.Mono;

public interface TemplateProvider {

    NotifyType getType();

    Provider getProvider();

    Mono<? extends Template> createTemplate(TemplateProperties properties);

    default ConfigMetadata getTemplateConfigMetadata() {
        return null;
    }
}
