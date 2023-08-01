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
