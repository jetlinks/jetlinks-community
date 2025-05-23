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
package org.jetlinks.community.notify.manager.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.notify.event.SerializableNotifierEvent;
import org.jetlinks.community.notify.manager.enums.NotifyState;
import org.jetlinks.community.utils.ObjectMappers;

import java.util.Map;

@Getter
@Setter
public class NotifyHistory {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "通知ID")
    private String notifierId;

    @Schema(description = "状态")
    private NotifyState state;

    @Schema(description = "错误类型")
    private String errorType;

    @Schema(description = "异常栈")
    private String errorStack;

    @Schema(description = "模版ID")
    private String templateId;

    @Schema(description = "模版内容")
    private String template;

    @Schema(description = "上下文")
    private Map<String, Object> context;

    @Schema(description = "服务商")
    private String provider;

    @Schema(description = "通知类型")
    private String notifyType;

    @Schema(description = "通知时间")
    private Long notifyTime;

    public static NotifyHistory of(SerializableNotifierEvent event) {
        NotifyHistory history = FastBeanCopier.copy(event, new NotifyHistory());
        if (null == event.getId()) {
            history.setId(IDGenerator.RANDOM.generate());
        }
        history.setNotifyTime(System.currentTimeMillis());
        if (null != event.getTemplate()) {
            history.setTemplate(ObjectMappers.toJsonString(event.getTemplate()));
        }
        if (event.isSuccess()) {
            history.setState(NotifyState.success);
        } else {
            history.setErrorStack(event.getCause());
            history.setState(NotifyState.error);
        }
        return history;
    }

    public JSONObject toJson() {
        JSONObject obj = FastBeanCopier.copy(this, new JSONObject());
        obj.put("state", state.getValue());
        obj.put("context", JSON.toJSONString(context));
        return obj;
    }

}
