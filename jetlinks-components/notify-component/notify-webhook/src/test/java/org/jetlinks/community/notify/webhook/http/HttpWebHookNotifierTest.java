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
package org.jetlinks.community.notify.webhook.http;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class HttpWebHookNotifierTest {

    private final HttpWebHookNotifier notifier = new HttpWebHookNotifier(
        "test",
        new HttpWebHookProperties(),
        WebClient.builder().baseUrl("https://example.com/api").build(),
        mock(org.jetlinks.community.notify.template.TemplateManager.class)
    );

    @Test
    void shouldAcceptRelativeTemplateUrl() {
        assertEquals(URI.create("events/device-1"), notifier.resolveTemplateUri("events/device-1"));
    }

    @Test
    void shouldRejectAbsoluteTemplateUrl() {
        assertThrows(
            IllegalArgumentException.class,
            () -> notifier.resolveTemplateUri("http://127.0.0.1/internal")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> notifier.resolveTemplateUri("//127.0.0.1/internal")
        );
    }
}
