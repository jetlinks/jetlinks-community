package org.jetlinks.community.rule.engine.executor;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import org.jetlinks.core.codec.defaults.JsonCodec;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@AllArgsConstructor
public class ReactorQLTaskExecutorProvider implements TaskExecutorProvider {

    private final EventBus eventBus;

    @Override
    public String getExecutor() {
        return "reactor-ql";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new ReactorQLTaskExecutor(context));
    }

    class ReactorQLTaskExecutor extends AbstractTaskExecutor {

        private ReactorQL reactorQL;

        public ReactorQLTaskExecutor(ExecutionContext context) {
            super(context);
            reactorQL = createQl();
        }

        @Override
        public String getName() {
            return "ReactorQL";
        }

        @Override
        protected Disposable doStart() {
            Flux<Object> dataStream;
            //有上游节点
            if (!CollectionUtils.isEmpty(context.getJob().getInputs())) {

                dataStream = context
                    .getInput()
                    .accept()
                    .flatMap(ruleData -> reactorQL
                        .start(Flux.just(RuleDataHelper.toContextMap(ruleData)))
                        .map(ruleData::newData)
                        .onErrorResume(err -> {
                            context.getLogger().error(err.getMessage(), err);
                            return context.onError(err, null).then(Mono.empty());
                        }));
            } else {
                dataStream = reactorQL
                    .start(table -> {
                        if (table == null || table.equalsIgnoreCase("dual")) {
                            return Flux.just(1);
                        }
                        if (table.startsWith("/")) {
                            //转换为消息
                            return eventBus
                                .subscribe(Subscription
                                               .builder()
                                               .subscriberId("rule-engine:"
                                                                 .concat(context.getInstanceId())
                                                                 .concat(":")
                                                                 .concat(context.getJob().getNodeId()))
                                               .topics(table)
                                               .local()
                                               .build())
                                .flatMap(payload -> {
                                    try {
                                        return Mono.just(payload.bodyToJson(true));
                                    } catch (Throwable error) {
                                        return context.onError(error, null);
                                    }
                                });
                        }
                        return Flux.just(1);
                    })
                    .cast(Object.class);
            }

            return dataStream
                .flatMap(result -> {
                    RuleData data = context.newRuleData(result);
                    //输出到下一节点
                    return context
                        .getOutput()
                        .write(Mono.just(data))
                        .then(context.fireEvent(RuleConstants.Event.result, data));
                })
                .onErrorResume(err -> context.onError(err, null))
                .subscribe();
        }

        protected ReactorQL createQl() {
            try {
                ReactorQL.Builder builder = Optional
                    .ofNullable(context.getJob().getConfiguration())
                    .map(map -> map.get("sql"))
                    .map(String::valueOf)
                    .map(ReactorQL.builder()::sql)
                    .orElseThrow(() -> new IllegalArgumentException("配置sql错误"));
                return builder.build();
            } catch (Exception e) {
                throw new IllegalArgumentException("SQL格式错误:" + e.getMessage(), e);
            }
        }

        @Override
        public void reload() {
            reactorQL = createQl();
            if (this.disposable != null) {
                this.disposable.dispose();
            }
            start();
        }

        @Override
        public void validate() {
            createQl();
        }
    }
}
