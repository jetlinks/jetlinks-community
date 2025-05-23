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
package org.jetlinks.community.notify.wechat.corp;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.notify.NotifierProperties;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class WechatCorpProperties {

    @NotBlank(message = "corpId不能为空")
    private String corpId;

    @NotBlank(message = "corpSecret不能为空")
    private String corpSecret;

    public static WechatCorpProperties of(NotifierProperties properties) {
        return FastBeanCopier.copy(properties.getConfiguration(), new WechatCorpProperties());
    }

}
