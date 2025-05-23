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
package org.jetlinks.community.gateway;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import org.jetlinks.rule.engine.executor.PayloadType;

import javax.annotation.Nonnull;
import java.util.Objects;

public class JsonEncodedMessage implements EncodableMessage {

    private volatile ByteBuf payload;

    @Getter
    private Object nativePayload;

    public JsonEncodedMessage(Object nativePayload) {
        Objects.requireNonNull(nativePayload);
        this.nativePayload = nativePayload;
    }

    @Nonnull
    @Override
    public ByteBuf getPayload() {
        if (payload == null) {
            payload = PayloadType.JSON.write(nativePayload);
        }
        return payload;
    }


}
