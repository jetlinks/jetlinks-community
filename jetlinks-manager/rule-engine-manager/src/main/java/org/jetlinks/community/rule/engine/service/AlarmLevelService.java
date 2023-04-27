package org.jetlinks.community.rule.engine.service;

import com.alibaba.fastjson.JSON;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.rule.engine.alarm.AlarmLevelInfo;
import org.jetlinks.community.rule.engine.entity.AlarmLevelEntity;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class AlarmLevelService extends GenericReactiveCrudService<AlarmLevelEntity, String> implements CommandLineRunner {

    public static final String DEFAULT_ALARM_ID = "default-alarm-id";


    private Mono<Void> initDefaultData() {
        return findById(DEFAULT_ALARM_ID)
            .switchIfEmpty(
                Mono.fromCallable(() -> {
                    ClassPathResource resource = new ClassPathResource("alarm-level.json");
                    try (InputStream stream = resource.getInputStream()) {
                        String json = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
                        List<AlarmLevelInfo> levelInfo = JSON.parseArray(json, AlarmLevelInfo.class);
                        return AlarmLevelEntity.defaultOf(levelInfo);
                    }
                })
                    .flatMap(e -> save(e)
                        .thenReturn(e))
            )
            .then();

    }


    @Override
    public void run(String... args) throws Exception {
        initDefaultData()
            .subscribe();
    }
}
