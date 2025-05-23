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
package org.jetlinks.community.things.metric;

import org.jetlinks.core.things.ThingId;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.stereotype.Component;

/**
 * 获取设备属性物模型指标
 * <pre>{@code
 *
 * select * from dual where
 * this.properties.temp > property.metric('device',deviceId,'temp','max')
 *
 * }</pre>
 *
 * @author zhouhao
 * @since 2.0
 */
@Component
public class MetricFunctionFeature extends FunctionMapFeature {

    public static String createFunction(String thingType,String thingId,String property,String metric){
        return "property.metric('"+thingType+"',"+thingId+",'"+property+"','"+metric+"')";
    }
    public MetricFunctionFeature(PropertyMetricManager metricManager) {
        super("property.metric", 4, 4, args -> {
            return args
                .collectList()
                .flatMap(argList -> {
                    String type = String.valueOf(argList.get(0));
                    String id = String.valueOf(argList.get(1));
                    String property = String.valueOf(argList.get(2));
                    String metric = String.valueOf(argList.get(3));

                    return metricManager
                        .getPropertyMetric(ThingId.of(type, id), property, metric)
                        .handle((m, sink) -> {
                            if (m.getValue() != null) {
                                sink.next(m.castValue());
                            }
                        });
                });
        });
    }
}
