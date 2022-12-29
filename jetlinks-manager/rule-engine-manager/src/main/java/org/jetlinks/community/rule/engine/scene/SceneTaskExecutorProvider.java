package org.jetlinks.community.rule.engine.scene;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.trace.TraceHolder;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.rule.engine.RuleEngineConstants;
import org.jetlinks.community.rule.engine.scene.term.limit.ShakeLimitGrouping;
import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@AllArgsConstructor
public class SceneTaskExecutorProvider implements TaskExecutorProvider {

    private static final int BACKPRESSURE_BUFFER_MAX_SIZE =
        Integer.getInteger("scene.backpressure-buffer-size", 10_0000);

    public static final String EXECUTOR = "scene";

    private final EventBus eventBus;

    private final SceneFilter filter;

    @Override
    public String getExecutor() {
        return "scene";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new SceneTaskExecutor(context));
    }

    class SceneTaskExecutor extends AbstractTaskExecutor {

        private SceneRule rule;

        public SceneTaskExecutor(ExecutionContext context) {
            super(context);
            load();
        }

        @Override
        public String getName() {
            return context.getJob().getName();
        }

        @Override
        protected Disposable doStart() {

            return disposable = init();
        }

        @Override
        public void validate() {
            rule.validate();
        }

        @Override
        public void reload() {
            load();
            doStart();
        }

        private void load() {
            SceneRule sceneRule = FastBeanCopier.copy(context.getJob().getConfiguration(),
                                                      new SceneRule());
            sceneRule.validate();
            this.rule = sceneRule;
        }

        private Object getDataId(Map<String, Object> data) {
            Object id;
            Object header = data.get("headers");
            if (header instanceof Map) {
                id = ((Map<?, ?>) header).get(PropertyConstants.uid.getKey());
            } else {
                id = data.get(PropertyConstants.uid.getKey());
            }
            if (null == id) {
                id = IDGenerator.RANDOM.generate();
            }
            return id;
        }

        private ReactorQLContext createReactorQLContext() {
            return ReactorQLContext
                .ofDatasource(table -> {
                    if (table.startsWith("/")) {
                        //来自事件总线
                        return this
                            .subscribe(table)
                            //有效期去重,同一个设备在多个部门的场景下,可能收到2条相同的数据问题
                            .as(FluxUtils.distinct(this::getDataId, Duration.ofSeconds(1)));
                    } else {
                        //来自上游(定时等)
                        return context
                            .getInput()
                            .accept()
                            .flatMap(RuleData::dataToMap);
                    }
                });
        }

        private Disposable init() {
            if (disposable != null) {
                disposable.dispose();
            }
            boolean useBranch = CollectionUtils.isNotEmpty(rule.getBranches());

            SqlRequest request = rule.createSql(!useBranch);
            Flux<Map<String, Object>> source;

            //不是通过SQL来处理数据
            if (request.isEmpty()) {
                source = context
                    .getInput()
                    .accept()
                    .flatMap(RuleData::dataToMap);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("init scene [{}:{}], sql:{}", rule.getId(), rule.getName(), request.toNativeSql());
                }

                ReactorQLContext qlContext = createReactorQLContext();

                //sql参数
                for (Object parameter : request.getParameters()) {
                    qlContext.bind(parameter);
                }
                source = ReactorQL
                    .builder()
                    .sql(request.getSql())
                    .build()
                    .start(qlContext)
                    .map(ReactorQLRecord::asMap);
            }

            // 分支条件
            if (useBranch) {
                return rule
                    .createBranchHandler(
                        source,
                        (idx, nodeId, data) -> {
                            if (log.isDebugEnabled()) {
                                log.debug("scene [{}] branch [{}] execute", rule.getId(), nodeId);
                            }
                            RuleData ruleData = context.newRuleData(data);
                            return context
                                .getOutput()
                                .write(nodeId, ruleData)
                                .onErrorResume(err -> context.onError(err, ruleData))
                                .as(tracer());
                        });
            }

            //防抖
            Trigger.GroupShakeLimit shakeLimit = rule.getTrigger().getShakeLimit();
            if (shakeLimit != null && shakeLimit.isEnabled()) {

                ShakeLimitGrouping<Map<String, Object>> grouping = shakeLimit.createGrouping();

                source = shakeLimit.transfer(
                    source,
                    (time, flux) -> grouping
                        .group(flux)
                        .flatMap(group -> group.window(time), Integer.MAX_VALUE),
                    (map, total) -> map.put("_total", total));
            }

            return source
                .flatMap(this::handleOutput)
                .subscribe();
        }

        private Flux<Map<String, Object>> subscribe(String topic) {
            return eventBus
                .subscribe(
                    Subscription
                        .builder()
                        .justLocal()
                        .topics(topic)
                        .subscriberId("scene:" + rule.getId())
                        .build())
                .handle((topicPayload, synchronousSink) -> {
                    try {
                        synchronousSink.next(topicPayload.bodyToJson(true));
                    } catch (Throwable err) {
                        log.warn("decode payload error {}", topicPayload.getTopic(), err);
                    }
                });
        }


        private Mono<Void> handleOutput(RuleData data) {
            return data
                .dataToMap()
                .filterWhen(map -> {
                    SceneData sceneData = new SceneData();
                    sceneData.setId(IDGenerator.SNOW_FLAKE_STRING.generate());
                    sceneData.setRule(rule);
                    sceneData.setOutput(map);

                    log.info("execute scene {} {} : {}", rule.getId(), rule.getName(), map);

                    return filter
                        .filter(sceneData)
                        .defaultIfEmpty(true);
                })
                .flatMap(map -> context
                    .getOutput()
                    .write(data.newData(map))
                    .as(tracer())
                    .contextWrite(ctx -> TraceHolder.readToContext(ctx, map)))
                .onErrorResume(err -> context.onError(err, data))
                .then();

        }

        private Mono<Void> handleOutput(Map<String, Object> data) {
            return handleOutput(context.newRuleData(data));
        }

        @Override
        public Mono<Void> execute(RuleData ruleData) {
            //分支
            if (CollectionUtils.isNotEmpty(rule.getBranches())) {
                if (log.isDebugEnabled()) {
                    log.debug("scene [{}] execute", rule.getId());
                }
                RuleData newData = context.newRuleData(ruleData);
                return context
                    .getOutput()
                    .write(newData)
                    .onErrorResume(err -> context.onError(err, ruleData))
                    .as(tracer())
                    .then();
            }
            return handleOutput(ruleData);
        }
    }
}
