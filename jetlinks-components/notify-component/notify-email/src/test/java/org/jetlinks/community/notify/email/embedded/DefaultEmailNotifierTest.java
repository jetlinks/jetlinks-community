/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.notify.email.embedded;

import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.notify.template.TemplateManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;

class DefaultEmailNotifierTest {

    @Test
    void shouldRejectRemoteAttachment() {
        DefaultEmailProperties properties = new DefaultEmailProperties();
        properties.setHost("localhost");
        properties.setPort(25);
        properties.setSender("test@example.com");

        DefaultEmailNotifier notifier = new DefaultEmailNotifier(
            "test",
            properties,
            mock(TemplateManager.class),
            mock(FileManager.class),
            WebClient.builder()
        );

        StepVerifier
            .create(notifier.convertResource("http://127.0.0.1/internal"))
            .expectError(UnsupportedOperationException.class)
            .verify();
    }
}
