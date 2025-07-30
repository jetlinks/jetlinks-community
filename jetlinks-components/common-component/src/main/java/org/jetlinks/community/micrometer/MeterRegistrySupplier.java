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
package org.jetlinks.community.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import org.jetlinks.core.metadata.DataType;

import java.util.Map;

public interface MeterRegistrySupplier {

    MeterRegistry getMeterRegistry(String metric, String... tagKeys);

    MeterRegistry getMeterRegistry(String metric, Map<String, DataType> tagDefine);

}
