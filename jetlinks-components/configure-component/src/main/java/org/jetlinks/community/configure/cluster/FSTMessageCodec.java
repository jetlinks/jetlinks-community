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
package org.jetlinks.community.configure.cluster;

import io.netty.util.concurrent.FastThreadLocal;
import io.scalecube.cluster.transport.api.Message;
import io.scalecube.cluster.transport.api.MessageCodec;
import lombok.AllArgsConstructor;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

@AllArgsConstructor
public class FSTMessageCodec implements MessageCodec {
    private final FastThreadLocal<FSTConfiguration> configuration;

    public FSTMessageCodec(Supplier<FSTConfiguration> supplier) {

        this(new FastThreadLocal<FSTConfiguration>() {
            @Override
            protected FSTConfiguration initialValue() {
                return supplier.get();
            }
        });
    }

    @Override
    public Message deserialize(InputStream stream) throws Exception {
        Message message = Message.builder().build();
        try (FSTObjectInput input = configuration.get().getObjectInput(stream)) {
            message.readExternal(input);
        }
        return message;
    }

    @Override
    public void serialize(Message message, OutputStream stream) throws Exception {
        try (FSTObjectOutput output = configuration.get().getObjectOutput(stream)) {
            message.writeExternal(output);
        }
    }
}
