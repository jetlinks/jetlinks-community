package org.jetlinks.community.rule.engine.scene;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@AllArgsConstructor
@Component
public class FlowLimitTaskExecutorProvider implements TaskExecutorProvider {

    public static final String EXECUTOR = "flow-limit";

    private static final Logger log = LoggerFactory.getLogger(FlowLimitTaskExecutorProvider.class);

    @Override
    public String getExecutor() {
        return EXECUTOR;
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext executionContext) {
        return Mono.just(new FlowLimitTaskExecutor(executionContext));
    }

    static class FlowLimitTaskExecutor extends FunctionTaskExecutor {

        private FlowLimitConfig config;

        public FlowLimitTaskExecutor(ExecutionContext context) {
            super("下发属性限流控制", context);
            reload();
        }

        @Override
        protected Publisher<RuleData> apply(RuleData ruleData) {
            List<String> properties = (List<String>) config.getMessage().get("properties");
            FlowLimitSpec flowLimitSpec = config.getFlowLimitSpec();
            long executeIntervalMillis = flowLimitSpec.getExecuteIntervalMillis(properties.size());

            return Flux
                .fromIterable(partition(properties, flowLimitSpec.getCount()))
                .delayElements(Duration.ofMillis(executeIntervalMillis))
                .map(item -> {
                    RuleData returnRuleData = FastBeanCopier.copy(ruleData, new RuleData(), "data");
                    Map<String, Object> map = new HashMap<>();
                    map.put("properties", item);
                    map.put("messageType", config.getMessage().get("messageType"));
                    map.put("timestamp", new Date().getTime());
                    returnRuleData.setData(map);
                    return returnRuleData;
                });
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new FlowLimitConfig());
        }


        public <T> List<List<T>> partition(List<T> list, int groupSize) {
            List<List<T>> partitions = new ArrayList<>();
            List<T> currentPartition = new ArrayList<>(groupSize);

            for (T item : list) {
                currentPartition.add(item);
                if (currentPartition.size() == groupSize) {
                    partitions.add(currentPartition);
                    currentPartition = new ArrayList<>(groupSize);
                }
            }

            // 添加最后一个可能不满的分区
            if (!currentPartition.isEmpty()) {
                partitions.add(currentPartition);
            }

            return partitions;
        }
    }

    @Getter
    @Setter
    public static class FlowLimitConfig {

        private FlowLimitSpec flowLimitSpec;

        private Map<String, Object> message;
    }
}
