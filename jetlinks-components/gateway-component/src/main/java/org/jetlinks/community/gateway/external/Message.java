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

public interface Message {

    String getRequestId();

    String getTopic();

    Object getPayload();

    String getMessage();

    Type getType();

    static Message authError() {

        return new SimpleMessage(null, null, null, Type.authError, "认证失败");
    }

    static Message error(String id, String topic, String message) {

        return new SimpleMessage(id, topic, null, Type.error, message);
    }

    static Message error(String id, String topic, Throwable message) {

        return new SimpleMessage(id, topic, null, Type.error, message.getMessage() == null ? message.getClass().getSimpleName() : message.getMessage());
    }

    static Message success(String id, String topic, Object payload) {
        return new SimpleMessage(id, topic, payload, Type.result, null);
    }

    static Message complete(String id) {
        return new SimpleMessage(id, null, null, Type.complete, null);
    }

    static Message pong(String id) {
        return new SimpleMessage(id, null, null, Type.pong, null);
    }

    enum Type {
        authError,
        result,
        error,
        complete,
        ping,
        pong
    }
}
