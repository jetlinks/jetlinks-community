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
package org.jetlinks.community.gateway.external;

import lombok.*;
import org.hswebframework.web.authorization.Authentication;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.gateway.external.socket.MessagingRequest;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeRequest implements ValueObject {

    private String id;

    private String topic;

    private Map<String, Object> parameter;

    private Authentication authentication;

    @Override
    public Map<String, Object> values() {
        return parameter;
    }


    public static SubscribeRequest of(MessagingRequest request,
                                      Authentication authentication) {
        return SubscribeRequest.builder()
            .id(request.getId())
            .topic(request.getTopic())
            .parameter(request.getParameter())
            .authentication(authentication)
            .build();

    }
}
