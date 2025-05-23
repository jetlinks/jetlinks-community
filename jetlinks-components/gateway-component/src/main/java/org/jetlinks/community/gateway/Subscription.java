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
package org.jetlinks.community.gateway;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 订阅信息.支持通配符**(匹配多层目录)和*(匹配单层目录).
 *
 * @author zhouhao
 * @since 1.0
 */
@Getter
@Setter
@EqualsAndHashCode(of = "topic")
public class Subscription {

    private String topic;

    public Subscription(String topic) {
        //适配mqtt topic通配符
        if (topic.contains("#") || topic.contains("+")) {
            topic = topic.replace("#", "**").replace("+", "*");
        }
        this.topic = topic;
    }

    public static Collection<Subscription> asList(String... sub) {
        return Stream.of(sub)
            .map(Subscription::new)
            .collect(Collectors.toList());
    }

}
