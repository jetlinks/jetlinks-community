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
package org.jetlinks.community.elastic.search.service.reactive;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.buffer.BufferProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "elasticsearch.buffer")
public class ElasticSearchBufferProperties extends BufferProperties {
    public ElasticSearchBufferProperties() {
        //固定缓冲文件目录
        setFilePath("./data/elasticsearch-buffer");
        setSize(3000);
    }

    private boolean refreshWhenWrite = false;

    private boolean ignoreDocId = false;
}