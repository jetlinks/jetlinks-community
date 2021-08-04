package org.jetlinks.community;

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.reactor.ql.utils.CastUtils;

public interface PropertyMetadataConstants {

    /**
     * 属性来源
     */
    interface Source {
        //数据来源
        String id = "source";

        //手动写值
        String manual = "manual";

        //规则,虚拟属性
        String rule = "rule";

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

        /**
         * 判断属性是否为规则
         *
         * @param metadata 物模型
         * @return 是否规则
         */
        static boolean isRule(PropertyMetadata metadata) {
            return  metadata
                .getExpand(id)
                .map(rule::equals)
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
}
