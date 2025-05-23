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
package org.jetlinks.community.timescaledb.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.FeatureId;
import org.hswebframework.ezorm.core.FeatureType;
import org.hswebframework.ezorm.core.meta.Feature;
import org.jetlinks.community.Interval;

@Getter
@AllArgsConstructor
public class CreateRetentionPolicy implements Feature, FeatureType {

    public static final FeatureId<CreateRetentionPolicy> ID = FeatureId.of("CreateRetentionPolicy");

    private final Interval interval;

    @Override
    public String getId() {
        return ID.getId();
    }

    @Override
    public String getName() {
        return "CreateRetentionPolicy";
    }

    @Override
    public FeatureType getType() {
        return this;
    }
}
