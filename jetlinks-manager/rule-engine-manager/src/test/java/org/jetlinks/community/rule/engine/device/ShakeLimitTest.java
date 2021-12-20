package org.jetlinks.community.rule.engine.device;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShakeLimitTest {

    @Test
    void wrapReactorQl() {
        ShakeLimit limit = new ShakeLimit();
        limit.setEnabled(false);
        String s = limit.wrapReactorQl("table", "test");
        assertNotNull(s);
        limit.setEnabled(true);
        limit.setTime(1);
        limit.setThreshold(1);
        limit.setAlarmFirst(true);
        String s1 = limit.wrapReactorQl("table", "test");
        assertNotNull(s1);
        assertTrue(limit.isEnabled());
    }

    @Test
    void transfer() {

        ShakeLimit shakeLimit = new ShakeLimit();
        shakeLimit.setEnabled(true);
        shakeLimit.setAlarmFirst(true);
        shakeLimit.setTime(1);
        shakeLimit.setThreshold(0);
        Map<String, Object> map = new HashMap<>();
        map.put("test","test");
        shakeLimit.transfer(Flux.just(map),(duration, flux)->flux.window(duration, Schedulers.parallel()),(alarm, total) -> alarm.put("totalAlarms", total))
            .map(s->s.get("test"))
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        shakeLimit.setEnabled(false);
        shakeLimit.setAlarmFirst(true);
        shakeLimit.setTime(0);
        shakeLimit.setThreshold(0);
        shakeLimit.transfer(Flux.just(map),(duration, flux)->flux.window(duration, Schedulers.parallel()),(alarm, total) -> alarm.put("totalAlarms", total))
            .map(s->s.get("test"))
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();
    }
}