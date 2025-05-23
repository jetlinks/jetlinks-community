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
package org.jetlinks.community.dashboard.web.response;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.dashboard.MeasurementDimension;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;

@Getter
@Setter
public class DimensionInfo {
    private String id;

    private String name;

    private DataType type;

    private ConfigMetadata params;

    private boolean realTime;

    public static DimensionInfo of(MeasurementDimension dimension) {
        DimensionInfo dimensionInfo = new DimensionInfo();
        dimensionInfo.setId(dimension.getDefinition().getId());
        dimensionInfo.setName(dimension.getDefinition().getName());
        dimensionInfo.setParams(dimension.getParams());
        dimensionInfo.setType(dimension.getValueType());
        dimensionInfo.setRealTime(dimension.isRealTime());
        return dimensionInfo;
    }
}
