package org.jetlinks.community.rule.engine.nodes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.node.RuleNodeConfig;
import org.reactivestreams.Publisher;
import org.springframework.scheduling.support.CronSequenceGenerator;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class TimerWorkerNode extends CommonExecutableRuleNodeFactoryStrategy<TimerWorkerNode.Configuration> {

    private Map<String, TimerWorkerNode.TimerJob> jobs = new ConcurrentHashMap<>();

    @Override
    public Function<RuleData, ? extends Publisher<?>> createExecutor(ExecutionContext context, Configuration config) {
        return Mono::just;
    }

    @Override
    protected void onStarted(ExecutionContext context, Configuration config) {
        super.onStarted(context, config);
        String id = context.getInstanceId() + ":" + context.getNodeId();

        context.onStop(() -> {
            TimerJob job = jobs.remove(id);
            if (null != job) {
                job.cancel();
            }
        });
        TimerJob job = jobs.computeIfAbsent(id, _id -> new TimerJob(config, context));

        job.doStart();
    }

    @Override
    public String getSupportType() {
        return "timer";
    }

    @AllArgsConstructor
    private static class TimerJob {
        private String id;
        private TimerWorkerNode.Configuration configuration;
        private ExecutionContext context;
        private volatile boolean running;

        TimerJob(TimerWorkerNode.Configuration configuration,
                 ExecutionContext context) {
            this.configuration = configuration;
            this.context = context;
            this.id = context.getInstanceId() + ":" + context.getNodeId();
        }


        void start() {
            running = true;
            doStart();
        }

        void doStart() {
            if (!running) {
                return;
            }
            running = true;
            Mono.delay(Duration.ofMillis(configuration.nextMillis()))
                .subscribe(t -> execute(this::start));
        }

        void execute(Runnable runnable) {
            if (!running) {
                return;
            }
            context.logger().debug("execute timer:{}", id);
            context.getOutput()
                .write(Mono.just(RuleData.create(System.currentTimeMillis())))
                .doOnError(err -> context.logger().error("fire timer error", err))
                .doFinally(s -> runnable.run())
                .subscribe();
        }

        void cancel() {
            running = false;
        }
    }


    public static class Configuration implements RuleNodeConfig {
        @Getter
        @Setter
        private String cron;

        private volatile CronSequenceGenerator generator;

        @Override
        public NodeType getNodeType() {
            return NodeType.PEEK;
        }

        @Override
        public void setNodeType(NodeType nodeType) {

        }

        public void init() {
            generator = new CronSequenceGenerator(cron);
        }

        @Override
        public void validate() {
            init();
        }

        public long nextMillis() {
            return Math.max(100, generator.next(new Date()).getTime() - System.currentTimeMillis());
        }

    }
}
