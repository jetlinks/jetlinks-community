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
package org.jetlinks.community.notify.webhook.http;

import org.jetlinks.community.notify.webhook.http.HttpWebHookTemplate;
import org.jetlinks.core.Values;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.util.Collections;

class HttpWebHookTemplateTest {


    @Test
    void testResolveBody() {
        HttpWebHookTemplate template = new HttpWebHookTemplate();
        template.setBody("{\"name\":\"${deviceName}\"}");

        String body = template.resolveBody(Values.of(Collections.singletonMap("deviceName", "Test")));
        System.out.println(body);
        Assertions.assertEquals(
            "{\"name\":\"Test\"}",
            body);

    }


    @Test
    void testResolveArrayBody() {
        HttpWebHookTemplate template = new HttpWebHookTemplate();
        template.setBody("[{\"name\":\"${deviceName}\"}]");

        String body = template.resolveBody(Values.of(Collections.singletonMap("deviceName", "Test")));
        System.out.println(body);
        Assertions.assertEquals("[{\"name\":\"Test\"}]", body);

    }

    @Test
    void testResolvePlainBody() {
        HttpWebHookTemplate template = new HttpWebHookTemplate();
        template.setBody("\"${deviceName}\"");

        String body = template.resolveBody(Values.of(Collections.singletonMap("deviceName", "Test")));
        System.out.println(body);
        Assertions.assertEquals("\"Test\"", body);

    }
}