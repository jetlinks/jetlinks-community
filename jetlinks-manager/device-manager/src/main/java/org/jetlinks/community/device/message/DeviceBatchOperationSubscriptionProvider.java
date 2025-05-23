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
package org.jetlinks.community.device.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Generated;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Component
@Generated
public class DeviceBatchOperationSubscriptionProvider implements SubscriptionProvider {

    private final LocalDeviceInstanceService instanceService;

    public DeviceBatchOperationSubscriptionProvider(LocalDeviceInstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @Override
    public String id() {
        return "device-batch-operator";
    }

    @Override
    public String name() {
        return "设备批量操作";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/device-batch/*"
        };
    }

    @Override
    public Flux<?> subscribe(SubscribeRequest request) {
        String topic = request.getTopic();

        @SuppressWarnings("all")
        QueryParamEntity queryParamEntity = request.get("query")
            .map(json -> {
                if (json instanceof Map) {
                    return new JSONObject(((Map<String, Object>) json));
                } else {
                    return JSON.parseObject(String.valueOf(json));
                }
            }).map(json -> json.toJavaObject(QueryParamEntity.class))
            .orElseGet(QueryParamEntity::new);

        Map<String, String> var = TopicUtils.getPathVariables("/device-batch/{type}", topic);
        String type = var.get("type");

        switch (type) {
            case "state-sync":
                return handleStateSync(queryParamEntity);
            case "deploy":
                return handleDeploy(queryParamEntity);

            default:
                return Flux.error(new IllegalArgumentException("不支持的类型:" + type));
        }

    }

    private Flux<?> handleDeploy(QueryParamEntity queryParamEntity) {

        return instanceService
            .query(queryParamEntity.noPaging().includes("id"))
            .as(instanceService::deploy);
    }

    private Flux<?> handleStateSync(QueryParamEntity queryParamEntity) {

        return instanceService.query(queryParamEntity.noPaging().includes("id"))
            .map(DeviceInstanceEntity::getId)
            .buffer(200)
            .publishOn(Schedulers.single())
            .concatMap(flux -> instanceService.syncStateBatch(Flux.just(flux), true))
            .flatMap(Flux::fromIterable);
    }
}
