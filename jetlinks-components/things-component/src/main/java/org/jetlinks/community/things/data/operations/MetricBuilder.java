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
package org.jetlinks.community.things.data.operations;

import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.metadata.PropertyMetadata;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface MetricBuilder {
    MetricBuilder DEFAULT = new MetricBuilder() {
    };

    /**
     * 获取自定义配置,通常由不同的存储策略来决定配置项
     *
     * @param key 配置KEY
     * @return 配置值
     */
    default <K> Optional<K> option(ConfigKey<K> key) {
        return Optional.empty();
    }

    /**
     * @return 物ID的字段标识, 默认为{@link ThingsDataConstants#COLUMN_THING_ID}
     */
    default String getThingIdProperty() {
        return ThingsDataConstants.COLUMN_THING_ID;
    }

    /**
     * @return 物模版ID的字段标识, 默认为{@link ThingsDataConstants#COLUMN_THING_ID}
     */
    default String getTemplateIdProperty() {
        return ThingsDataConstants.COLUMN_TEMPLATE_ID;
    }

    /**
     * 创建日志存储的度量名称
     *
     * @param thingType       物类型
     * @param thingTemplateId 物模版ID(设备产品ID)
     * @param thingId         物ID
     * @return 度量名
     */
    default String createLogMetric(@Nonnull String thingType,
                                   @Nonnull String thingTemplateId,
                                   String thingId) {
        return thingType + "_log_" + thingTemplateId;
    }

    /**
     * 创建日志存储的度量名称,当{@link DataSettings#getLogFilter()},
     * {@link DataSettings.Log#isAllInOne()}为true时调用.
     *
     * @param thingType 物类型
     * @return 度量名
     */
    default String createLogMetric(@Nonnull String thingType) {
        return thingType + "_all_log";
    }


    /**
     * 创建属性存储的度量名称
     *
     * @param thingType       物类型
     * @param thingTemplateId 物模版ID(设备产品ID)
     * @param thingId         物ID
     * @return 度量名
     */
    default String createPropertyMetric(@Nonnull String thingType,
                                        @Nonnull String thingTemplateId,
                                        String thingId) {
        return thingType + "_properties_" + thingTemplateId;
    }

    /**
     * 创建属性存储的度量名称
     *
     * @param thingType       物类型
     * @param thingTemplateId 物模版ID(设备产品ID)
     * @param thingId         物ID
     * @param group 物模型分组
     * @see ThingsDataConstants#propertyGroup(PropertyMetadata)
     * @return 度量名
     */
    default String createPropertyMetric(@Nonnull String thingType,
                                        @Nonnull String thingTemplateId,
                                        String thingId,
                                        String group) {
        return thingType + "_properties_" + group + "_" + thingTemplateId;
    }

    /**
     * 创建事件存储的度量名称,用于存储所有事件数据
     *
     * @param thingType       物类型
     * @param thingTemplateId 物模版ID(设备产品ID)
     * @param thingId         物ID
     * @return 度量名
     */
    default String createEventAllInOneMetric(@Nonnull String thingType,
                                             @Nonnull String thingTemplateId,
                                             String thingId) {
        return thingType + "_event_" + thingTemplateId + "_events";
    }

    /**
     * 创建事件存储的度量名称,用于存储单个事件数据
     *
     * @param thingType       物类型
     * @param thingTemplateId 物模版ID(设备产品ID)
     * @param thingId         物ID
     * @param eventId         事件ID
     * @return 度量名
     */
    default String createEventMetric(@Nonnull String thingType,
                                     @Nonnull String thingTemplateId,
                                     String thingId,
                                     @Nonnull String eventId) {
        return thingType + "_event_" + thingTemplateId + "_" + eventId;
    }
}
