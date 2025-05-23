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
package org.jetlinks.community.rule.engine.executor.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.relation.utils.VariableSource;
import org.jetlinks.community.rule.engine.commons.things.ThingInfoSpec;
import org.jetlinks.community.rule.engine.executor.DeviceSelector;
import org.jetlinks.community.rule.engine.executor.DeviceSelectorBuilder;
import org.jetlinks.core.device.DeviceState;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.things.ThingsDataManager;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 *
 * @author zhangji 2025/1/22
 * @since 2.3
 */
@AllArgsConstructor
public class DeviceDataTaskExecutorProvider implements TaskExecutorProvider {

    public static final String ID = "device-info";

    private final ThingsDataManager dataManager;
    private final DeviceSelectorBuilder selectorBuilder;

    @Override
    public String getExecutor() {
        return ID;
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new ThingInfoTaskExecutor(context, dataManager, selectorBuilder));
    }

    static class ThingInfoTaskExecutor extends FunctionTaskExecutor {

        private Config config;
        private DeviceSelector selector;
        private final ThingsDataManager dataManager;
        private final DeviceSelectorBuilder selectorBuilder;

        public ThingInfoTaskExecutor(ExecutionContext context,
                                     ThingsDataManager dataManager,
                                     DeviceSelectorBuilder selectorBuilder) {
            super("获取设备信息", context);
            this.dataManager = dataManager;
            this.selectorBuilder = selectorBuilder;
            reload();
        }

        @Override
        public void reload() {
            this.config = createConfig();
            this.selector = selectorBuilder.createSelector(config.getSelector());
        }

        @Override
        protected Publisher<RuleData> apply(RuleData input) {
            Map<String, Object> ctx = RuleDataHelper.toContextMap(input);
            long ts = config.resolveTime(ctx);
            return selector
                .select(ctx)
                .flatMap(device -> config
                    .read(device, dataManager, ts)
                    .map(output -> context.newRuleData(input, output)));
        }

        private Config createConfig() {
            return FastBeanCopier.copy(context.getJob().getConfiguration(), new Config());
        }
    }


    @Getter
    @Setter
    public static class Config extends ThingInfoSpec {
        @Schema(title = "设备选择器")
        private DeviceSelectorSpec selector;

        @Schema(title = "基准时间来源")
        private VariableSource baseTime;

        @Override
        protected ObjectType createConfigType() {
            return new ObjectType()
                .addProperty("state",
                             LocaleUtils.resolveMessage("message.thing.info.spec.config.state", "在线状态"),
                             new EnumType()
                                 .addElement(EnumType.Element.of(
                                     String.valueOf(DeviceState.online),
                                     LocaleUtils.resolveMessage("message.thing.info.spec.config.state-online", "在线")))
                                 .addElement(EnumType.Element.of(
                                     String.valueOf(DeviceState.offline),
                                     LocaleUtils.resolveMessage("message.thing.info.spec.config.state-offline", "离线")))
                )
                .addProperty("onlineTime",
                             LocaleUtils.resolveMessage("message.thing.info.spec.config.onlineTime", "上一次在线时间"),
                             DateTimeType.GLOBAL
                )
                .addProperty("offlineTime",
                             LocaleUtils.resolveMessage("message.thing.info.spec.config.offlineTime", "上一次离线时间"),
                             DateTimeType.GLOBAL
                )
                .addProperty(PropertyConstants.deviceName.getKey(),
                             LocaleUtils.resolveMessage("message.thing.info.spec.config.deviceName", "设备名称"),
                             StringType.GLOBAL
                )
                .addProperty(PropertyConstants.productName.getKey(),
                             LocaleUtils.resolveMessage("message.thing.info.spec.config.productName", "产品名称"),
                             StringType.GLOBAL
                );
        }

        long resolveTime(Map<String, Object> ctx) {
            if (baseTime != null) {
                Number time = CastUtils.castNumber(baseTime.resolveStatic(ctx), (ignore) -> null);
                if (time != null) {
                    return time.longValue();
                }
            }
            return System.currentTimeMillis();
        }
    }
}
