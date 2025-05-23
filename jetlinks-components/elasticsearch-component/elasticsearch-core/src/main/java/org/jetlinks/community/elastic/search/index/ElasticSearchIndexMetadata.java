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
package org.jetlinks.community.elastic.search.index;

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.elastic.search.utils.ElasticSearchConverter;

import java.util.List;
import java.util.Map;

public interface ElasticSearchIndexMetadata {

    String getIndex();

    List<PropertyMetadata> getProperties();

    PropertyMetadata getProperty(String property);

    default Map<String, Object> convertToElastic(Map<String, Object> map) {
        return ElasticSearchConverter.convertDataToElastic(map, getProperties());
    }

    default Map<String, Object> convertFromElastic(Map<String, Object> map) {
        return ElasticSearchConverter.convertDataFromElastic(map, getProperties());
    }

    default ElasticSearchIndexMetadata newIndexName(String name) {
        return new DefaultElasticSearchIndexMetadata(name, getProperties());
    }
}
