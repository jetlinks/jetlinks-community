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
package org.jetlinks.community.network;

import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
public enum PubSubType implements EnumDict<String> {

    producer,
    consumer;

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getText() {
        return name();
    }
}
