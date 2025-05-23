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
package org.jetlinks.community.timeseries;

import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.Arrays;
import java.util.List;

public interface TimeSeriesMetadata {

    TimeSeriesMetric getMetric();

    List<PropertyMetadata> getProperties();

    static TimeSeriesMetadata of(TimeSeriesMetric metric, PropertyMetadata... properties) {
        return of(metric, Arrays.asList(properties));
    }

    static TimeSeriesMetadata of(TimeSeriesMetric metric, List<PropertyMetadata> properties) {
        return new TimeSeriesMetadata() {
            @Override
            public TimeSeriesMetric getMetric() {
                return metric;
            }

            @Override
            public List<PropertyMetadata> getProperties() {
                return properties;
            }
        };
    }
}
