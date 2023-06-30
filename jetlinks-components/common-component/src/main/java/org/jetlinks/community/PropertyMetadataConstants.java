package org.jetlinks.community;

import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.HeaderKey;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.reactor.ql.utils.CastUtils;

import java.util.*;

public interface PropertyMetadataConstants {


    /**
     * 属性来源
     */
    interface Source {

        //数据来源
        String id = "source";

        HeaderKey<String> headerKey = HeaderKey.of(id, null);

        //手动写值
        String manual = "manual";

        //规则,虚拟属性
        String rule = "rule";

        static boolean isManual(DeviceMessage message) {
            return message
                .getHeader(Source.headerKey)
                .map(Source.manual::equals)
                .orElse(false);
        }

        static void setManual(DeviceMessage message) {
            message.addHeader(headerKey, manual);
        }

        /**
         * 判断属性是否手动赋值
         *
         * @param metadata 属性物模型
         * @return 是否手动赋值
         */
        static boolean isManual(PropertyMetadata metadata) {
            return metadata.getExpand(id)
                           .map(manual::equals)
                           .orElse(false);
        }

    }

    /**
     * 属性读写模式
     */
    interface AccessMode {
        String id = "accessMode";

        //读
        String read = "r";
        //写
        String write = "w";
        //上报
        String report = "u";

        static boolean isRead(PropertyMetadata property) {
            return property
                .getExpand(id)
                .map(val -> val.toString().contains(read))
                .orElse(true);
        }

        static boolean isWrite(PropertyMetadata property) {
            return property
                .getExpand(id)
                .map(val -> val.toString().contains(write))
                .orElseGet(() -> property
                    .getExpand("readOnly")
                    .map(readOnly -> !CastUtils.castBoolean(readOnly))
                    .orElse(true)
                );
        }

        static boolean isReport(PropertyMetadata property) {
            return property
                .getExpand(id)
                .map(val -> val.toString().contains(report))
                .orElse(true);
        }
    }

    interface Metrics {
        String id = "metrics";


        static Map<String,Object> metricsToExpands(List<PropertyMetric> metrics) {
            return Collections.singletonMap(id, metrics);
        }

        static List<PropertyMetric> getMetrics(PropertyMetadata metadata) {
            return metadata
                .getExpand(id)
                .map(obj -> ConverterUtils.convertToList(obj, PropertyMetric::of))
                .orElseGet(Collections::emptyList);
        }

        static Optional<PropertyMetric> getMetric(PropertyMetadata metadata, String metric) {
            return metadata
                .getExpand(id)
                .map(obj -> {
                    for (PropertyMetric propertyMetric : ConverterUtils.convertToList(obj, PropertyMetric::of)) {
                        if(Objects.equals(metric, propertyMetric.getId())){
                            return propertyMetric;
                        }
                    }
                    return null;
                });
        }

    }
}
