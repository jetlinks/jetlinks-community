package org.jetlinks.community.device.service.data;

import lombok.AllArgsConstructor;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.message.property.PropertyMessage;
import org.jetlinks.core.things.ThingProperty;
import org.jetlinks.core.things.ThingType;
import org.jetlinks.core.things.ThingsDataManagerSupport;
import org.jetlinks.community.things.ThingConstants;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Component
public class DeviceDataManagerSupport implements ThingsDataManagerSupport {
    private final DeviceDataService dataService;

    private final EventBus eventBus;

    @Override
    public boolean isSupported(ThingType thingType) {
        return true;
    }

    @Override
    public Mono<ThingProperty> getFirstProperty(ThingType thingType, String thingId) {
        // FIXME: 2021/11/2 使用缓存
        // order by timestamp asc limit 0,1
        return QueryParamEntity
            .newQuery()
            .where()
            .doPaging(0, 1)
            .orderByAsc("timestamp")
            .execute(param -> dataService.queryProperty(thingId, param))
            .take(1)
            .map(data -> ThingProperty.of(data.getProperty(), data.getValue(), data.getTimestamp(), data.getState()))
            .singleOrEmpty();
    }

    @Override
    public Mono<ThingProperty> getAnyLastProperty(ThingType thingType, String thingId, long baseTime) {
        // where timestamp < ? order by timestamp desc limit 0,1
        return QueryParamEntity
            .newQuery()
            .where()
            .lt("timestamp", baseTime)
            .doPaging(0, 1)
            .orderByDesc("timestamp")
            .execute(param -> dataService.queryProperty(thingId, param))
            .take(1)
            .map(data -> ThingProperty.of(data.getProperty(), data.getValue(), data.getTimestamp(), data.getState()))
            .singleOrEmpty();
    }

    @Override
    public Mono<ThingProperty> getLastProperty(ThingType thingType, String thingId, String property, long baseTime) {
        // where property = ? and timestamp < ? order by timestamp desc limit 0,1
        return QueryParamEntity
            .newQuery()
            .where()
            .lt("timestamp", baseTime)
            .doPaging(0, 1)
            .orderByDesc("timestamp")
            .execute(param -> dataService.queryProperty(thingId, param, property))
            .take(1)
            .map(data -> ThingProperty.of(data.getProperty(), data.getValue(), data.getTimestamp(), data.getState()))
            .singleOrEmpty();
    }

    @Override
    public Mono<ThingProperty> getFirstProperty(ThingType thingType, String thingId, String property) {
        // where property = ? order by timestamp asc limit 0,1
        return QueryParamEntity
            .newQuery()
            .where()
            .doPaging(0, 1)
            .orderByAsc("timestamp")
            .execute(param -> dataService.queryProperty(thingId, param, property))
            .take(1)
            .map(data -> ThingProperty.of(data.getProperty(), data.getValue(), data.getTimestamp(), data.getState()))
            .singleOrEmpty();
    }

    private final static Codec<ThingMessage> CODEC = Codecs.lookup(ThingMessage.class);

    @Override
    public Flux<ThingProperty> subscribeProperty(ThingType thingType, String thingId) {
        return eventBus
            .subscribe(Subscription
                           .builder()
                           .topics(ThingConstants.Topics.properties(thingType, thingId))
                           .subscriberId("device-data:" + thingId)
                           .local()
                           .broker()
                           .build(),
                       CODEC)
            .cast(PropertyMessage.class)
            .flatMapIterable(PropertyMessage::getCompleteProperties);
    }
}
