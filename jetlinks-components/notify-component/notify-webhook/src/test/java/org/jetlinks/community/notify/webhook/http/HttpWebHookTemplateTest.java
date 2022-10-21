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