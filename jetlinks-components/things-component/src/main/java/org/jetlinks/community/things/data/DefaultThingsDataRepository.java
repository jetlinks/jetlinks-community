package org.jetlinks.community.things.data;

import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.things.ThingTemplate;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.ThingsDataRepository;
import org.jetlinks.community.things.data.operations.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 默认物数据服务,支持动态数据存储策略{@link ThingsDataRepositoryStrategy},
 * <p>
 * 支持不同物类型自定义表名逻辑{@link ThingsDataCustomizer#custom(ThingsDataContext)} {@link ThingsDataContext#customMetricBuilder(String, MetricBuilder)}
 *
 * @author zhouhao
 * @since 2.0
 */
public class DefaultThingsDataRepository implements ThingsDataRepository, ThingsDataContext, SaveOperations {

    private final Map<String, ThingsDataRepositoryStrategy> policies = new ConcurrentHashMap<>();

    private final Map<String, ThingsDataRepositoryStrategy.OperationsContext> contexts = new ConcurrentHashMap<>();

    private final ThingsRegistry registry;

    private String defaultPolicy = "default-row";

    private ThingsDataRepositoryStrategy.OperationsContext defaultContext = new ThingsDataRepositoryStrategy.OperationsContext(
        MetricBuilder.DEFAULT, new DataSettings()
    );

    public DefaultThingsDataRepository(ThingsRegistry registry) {
        this.registry = registry;
    }

    private ThingsDataRepositoryStrategy getPolicyNow(String policy) {
        ThingsDataRepositoryStrategy dataPolicy = policies.get(policy);
        if (dataPolicy == null) {
            throw new I18nSupportException("error.thing_data_policy_unsupported", policy);
        }
        return dataPolicy;
    }

    private Mono<Tuple2<String, ThingsDataRepositoryStrategy>> getPolicyByThing(String thingType, String thingId) {
        return registry
            .getThing(thingType, thingId)
            .flatMap(thing -> Mono
                .zip(
                    thing.getTemplate().map(ThingTemplate::getId),
                    thing
                        .getConfig(ThingsDataConstants.storePolicyConfigKey)
                        .defaultIfEmpty(defaultPolicy)
                        .map(this::getPolicyNow)
                )
            );
    }

    private Mono<ThingsDataRepositoryStrategy> getPolicyByTemplate(String thingType, String templateId) {
        return registry
            .getTemplate(thingType, templateId)
            .flatMap(template -> template
                .getConfig(ThingsDataConstants.storePolicyConfigKey)
                .defaultIfEmpty(defaultPolicy))
            .map(this::getPolicyNow);
    }

    @Override
    public SaveOperations opsForSave() {
        return this;
    }

    @Override
    public Mono<ThingOperations> opsForThing(String thingType, String thingId) {

        return this
            .getPolicyByThing(thingType, thingId)
            .map((tp2) -> tp2
                .getT2()
                .opsForThing(thingType, tp2.getT1(), thingId, contexts.getOrDefault(thingType, defaultContext)));
    }

    @Override
    public Mono<TemplateOperations> opsForTemplate(String thingType, String templateId) {
        return this
            .getPolicyByTemplate(thingType, templateId)
            .map(policy -> policy.opsForTemplate(thingType, templateId, contexts.getOrDefault(thingType, defaultContext)));
    }

    @Override
    public void customMetricBuilder(String thingType, MetricBuilder metricBuilder) {
        contexts.compute(thingType, (k, old) -> {
            if (old == null) {
                return new ThingsDataRepositoryStrategy.OperationsContext(metricBuilder, defaultContext.getSettings());
            }
            return old.metricBuilder(metricBuilder);
        });

    }

    @Override
    public void customSettings(String thingType, DataSettings settings) {

        contexts.compute(thingType, (k, old) -> {
            if (old == null) {
                return new ThingsDataRepositoryStrategy.OperationsContext(defaultContext.getMetricBuilder(), settings);
            }
            return old.settings(settings);
        });
    }


    @Override
    public void setDefaultPolicy(String policy) {
        this.defaultPolicy = policy;
    }

    @Override
    public void setDefaultSettings(DataSettings settings) {
        for (Map.Entry<String, ThingsDataRepositoryStrategy.OperationsContext> entry : contexts.entrySet()) {
            if (entry.getValue().getSettings() == defaultContext.getSettings()) {
                entry.setValue(new ThingsDataRepositoryStrategy.OperationsContext(entry.getValue().getMetricBuilder(), settings));
            }
        }
        this.defaultContext = new ThingsDataRepositoryStrategy.OperationsContext(MetricBuilder.DEFAULT, settings);
    }

    @Override
    public void addPolicy(ThingsDataRepositoryStrategy policy) {
        ThingsDataRepositoryStrategies.register(policy);
        policies.put(policy.getId(), policy);
    }

    @Override
    public Mono<Void> save(ThingMessage thingMessage) {
        return doSave(thingMessage.getThingType(),
                      thingMessage.getThingId(),
                      opt -> opt.save(thingMessage));
    }

    @Override
    public Mono<Void> save(Collection<? extends ThingMessage> thingMessage) {
        return save(Flux.fromIterable(thingMessage));
    }

    @Override
    public Mono<Void> save(Publisher<? extends ThingMessage> thingMessage) {
        return Flux.from(thingMessage)
                   .groupBy(msg -> Tuples.of(msg.getThingType(), msg.getThingId()))
                   .flatMap(group -> doSave(group.key().getT1(), group.key().getT2(), opt -> opt.save(group)))
                   .then();
    }

    private Mono<Void> doSave(String thingType, String thingId, Function<SaveOperations, Mono<Void>> opt) {
        return this
            .getPolicyByThing(thingType, thingId)
            .flatMap((tp2) -> opt.apply(tp2.getT2().opsForSave(contexts.getOrDefault(thingType, defaultContext))));
    }
}
