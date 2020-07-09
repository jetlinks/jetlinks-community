package org.jetlinks.community.rule.engine.service;

import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmHistoryEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

@Service
public class DeviceAlarmHistoryService extends GenericReactiveCrudService<DeviceAlarmHistoryEntity, String> {


    EmitterProcessor<DeviceAlarmHistoryEntity> processor = EmitterProcessor.create(false);
    FluxSink<DeviceAlarmHistoryEntity> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    @PostConstruct
    public void init() {
        FluxUtils.bufferRate(processor, 500, 200, Duration.ofSeconds(2))
            .flatMap(list -> this.insertBatch(Mono.just(list)))
            .subscribe();
    }

    @Subscribe("/rule-engine/device/alarm/**")
    public Mono<Void> saveAlarm(Map<String, Object> message) {
        DeviceAlarmHistoryEntity entity = FastBeanCopier.copy(message, DeviceAlarmHistoryEntity::new);
        if (message.containsKey("timestamp")) {
            entity.setAlarmTime(new Date((Long) message.get("timestamp")));
        }
        entity.setAlarmData(message);
        return Mono
            .fromRunnable(() -> sink.next(entity));
    }

}
