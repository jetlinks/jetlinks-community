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
package org.jetlinks.community.dashboard.web.request;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.dashboard.Dashboard;
import org.jetlinks.community.dashboard.DashboardDefinition;
import org.jetlinks.community.dashboard.DimensionDefinition;
import org.jetlinks.community.dashboard.MeasurementDefinition;
import org.jetlinks.community.dashboard.web.response.DashboardMeasurementResponse;

import java.util.Map;

/**
 * 仪表盘指标数据请求
 *
 * @author zhouhao
 * @since 1.0
 */
@Getter
@Setter
public class DashboardMeasurementRequest {

    /**
     * 分组
     * @see DashboardMeasurementResponse#getGroup()
     */
    private String group;

    /**
     * 仪表盘,如: device
     * @see Dashboard#getDefinition()
     */
    private String dashboard;

    /**
     * 仪表对象,如: device1
     * @see  DashboardDefinition#getId()
     */
    private String object;

    /**
     * 指标,如: 属性ID
     * @see  MeasurementDefinition#getId()
     */
    private String measurement;

    /**
     * 维度
     * @see DimensionDefinition#getId()
     */
    private String dimension;

    /**
     * 查询参数
     */
    private Map<String, Object> params;

}
