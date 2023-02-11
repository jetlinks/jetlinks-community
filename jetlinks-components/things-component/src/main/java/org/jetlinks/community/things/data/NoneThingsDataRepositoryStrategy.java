package org.jetlinks.community.things.data;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.metadata.Feature;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.community.things.data.operations.*;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.reactivestreams.Publisher;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collection;

public class NoneThingsDataRepositoryStrategy implements
    ThingsDataRepositoryStrategy,
    SaveOperations,
    ThingOperations,
    TemplateOperations,
    QueryOperations,
    DDLOperations {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Flux<Feature> getFeatures() {
        return SaveOperations.super.getFeatures();
    }

    @Override
    public String getId() {
        return "none";
    }

    @Override
    public String getName() {
        return "不存储";
    }

    @Override
    public SaveOperations opsForSave(OperationsContext context) {
        return this;
    }

    @Override
    public ThingOperations opsForThing(String thingType, String templateId, String thingId, OperationsContext context) {
        return this;
    }

    @Override
    public TemplateOperations opsForTemplate(String thingType, String templateId, OperationsContext context) {
        return this;
    }

    @Override
    public Mono<Void> save(ThingMessage thingMessage) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> save(Collection<? extends ThingMessage> thingMessage) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> save(Publisher<? extends ThingMessage> thingMessage) {
        return Mono.empty();
    }

    @Override
    public QueryOperations forQuery() {
        return this;
    }

    @Override
    public DDLOperations forDDL() {
        return this;
    }

    @Nonnull
    @Override
    public Flux<ThingPropertyDetail> queryEachProperty(@Nonnull QueryParamEntity query, @Nonnull String... property) {
        return Flux.empty();
    }

    @Nonnull
    @Override
    public Flux<ThingPropertyDetail> queryProperty(@Nonnull QueryParamEntity query, @Nonnull String... property) {
        return Flux.empty();
    }

    @Nonnull
    @Override
    public Mono<PagerResult<ThingPropertyDetail>> queryPropertyPage(@Nonnull QueryParamEntity query, @Nonnull String... property) {
        return Mono.just(PagerResult.empty());
    }

    @Nonnull
    @Override
    public Flux<AggregationData> aggregationProperties(@Nonnull AggregationRequest request, @Nonnull PropertyAggregation... properties) {
        return Flux.empty();
    }

    @Override
    public Flux<ThingMessageLog> queryMessageLog(@Nonnull QueryParamEntity query) {
        return Flux.empty();
    }

    @Override
    public Mono<PagerResult<ThingMessageLog>> queryMessageLogPage(@Nonnull QueryParamEntity query) {
        return Mono.just(PagerResult.empty());
    }

    @Nonnull
    @Override
    public Mono<PagerResult<ThingEvent>> queryEventPage(@Nonnull String eventId, @Nonnull QueryParamEntity query, boolean format) {
        return Mono.just(PagerResult.empty());
    }

    @Nonnull
    @Override
    public Flux<ThingEvent> queryEvent(@Nonnull String eventId, @Nonnull QueryParamEntity query, boolean format) {
        return Flux.empty();
    }

    @Override
    public Mono<Void> registerMetadata(ThingMetadata metadata) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> reloadMetadata(ThingMetadata metadata) {
        return Mono.empty();
    }
}
