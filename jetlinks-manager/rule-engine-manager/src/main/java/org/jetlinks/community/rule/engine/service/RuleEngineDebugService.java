package org.jetlinks.community.rule.engine.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.cluster.ClusterQueue;
import org.jetlinks.core.cluster.ClusterTopic;
import org.jetlinks.rule.engine.api.*;
import org.jetlinks.rule.engine.api.executor.*;
import org.jetlinks.rule.engine.api.model.Condition;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.cluster.logger.ClusterLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Service
public class RuleEngineDebugService {

    @Autowired
    private ExecutableRuleNodeFactory executableRuleNodeFactory;

    @Autowired
    private RuleEngineModelParser modelParser;

    @Autowired
    private ConditionEvaluator conditionEvaluator;

    private Map<String, Session> sessionStore = new ConcurrentHashMap<>();


    public Flux<DebugMessage> getDebugMessages(String sessionId) {
        return getSession(sessionId)
            .consumeOutPut();
    }

    private Session getSession(String id) {
        return Optional.ofNullable(id)
            .map(sessionStore::get)
            .orElseThrow(() -> new NotFoundException("session不存在"));
    }

    public Mono<String> startSession() {
        return Mono.fromSupplier(() -> {
            String sessionId = IDGenerator.UUID.generate();
            Session session = new Session(sessionId);
            session.local = true;
            sessionStore.put(sessionId, session);
            return sessionId;
        });
    }


    @SneakyThrows
    public String startNode(String sessionId, RuleNodeConfiguration configuration) {
        configuration.setNodeType(NodeType.MAP);
        Session session = getSession(sessionId);

        DebugExecutionContext context = session.createContext(configuration);

        ExecutableRuleNode ruleNode = executableRuleNodeFactory.create(configuration);

        ruleNode.start(context);

        return context.id;
    }

    public void sendData(String sessionId, String contextId, RuleData ruleData) {
        getSession(sessionId)
            .getContext(contextId)
            .execute(ruleData);
    }


    public Mono<Boolean> stopContext(String sessionId, String contextId) {

        return Mono.fromRunnable(() -> getSession(sessionId).stopContext(contextId))
            .thenReturn(true);
    }

    public Set<String> getAllContext(String sessionId) {
        return getSession(sessionId)
            .contexts
            .keySet();
    }

    public Mono<Boolean> closeSession(String sessionId) {
        return Mono.fromRunnable(() -> getSession(sessionId).close());
    }

    public Mono<Boolean> testCondition(String sessionId, Condition condition, Object data) {

        Session session = getSession(sessionId);

        try {
            boolean success = conditionEvaluator.evaluate(condition, RuleData.create(data));
            return session.writeMessage(DebugMessage.of("output", null, "测试条件:".concat(success ? "通过" : "未通过")));
        } catch (Exception e) {
            return session.writeMessage(DebugMessage.of("error", null, StringUtils.throwable2String(e)));
        }
    }

    private class Session {
        private String id;

        private long lastOperationTime;

        private Map<String, DebugExecutionContext> contexts = new ConcurrentHashMap<>();

        private Map<String, RuleInstanceContext> instanceContext = new ConcurrentHashMap<>();

        private Map<String, String> instanceContextMapping = new ConcurrentHashMap<>();

        private EmitterProcessor<DebugMessage> messageQueue = EmitterProcessor.create(false);

        @Getter
        private boolean local = false;

        private Session(String id) {
            this.lastOperationTime = System.currentTimeMillis();
            this.id = id;
        }

        private boolean isTimeout() {
            return System.currentTimeMillis() - lastOperationTime > TimeUnit.MINUTES.toMillis(15);
        }

        private void checkContextTimeout() {
            contexts.entrySet()
                .stream()
                .filter(e -> e.getValue().isTimeout())
                .map(Map.Entry::getKey)
                .map(contexts::remove)
                .forEach(DebugExecutionContext::stop);
        }

        private void stopContext(String contextId) {
            Optional.ofNullable(contexts.remove(contextId))
                .ifPresent(ExecutionContext::stop);
        }

        private Logger createLogger(String contextId, String nodeId) {
            ClusterLogger logger = new ClusterLogger();
            logger.setParent(new Slf4jLogger("rule.engine.debug.".concat(nodeId)));
            logger.setNodeId(nodeId);
            logger.setInstanceId(contextId);
            logger.setLogInfoConsumer(logInfo -> {

                Map<String, Object> data = new HashMap<>();
                data.put("level", logInfo.getLevel());
                data.put("message", logInfo.getMessage());

                writeMessage(DebugMessage.of("log", contextId, data))
                    .subscribe();

            });
            return logger;
        }

        private DebugExecutionContext createContext(RuleNodeConfiguration configuration) {
            lastOperationTime = System.currentTimeMillis();
            String id = Optional.ofNullable(configuration.getId()).orElseGet(IDGenerator.MD5::generate);
            DebugExecutionContext context = contexts.get(id);
            if (context != null) {
                context.stop();
                contexts.remove(id);
            }

            context = new DebugExecutionContext(id, createLogger(id, configuration.getNodeId()), this);
            context.local = true;
            contexts.put(id, context);
            return context;
        }

        private DebugExecutionContext getContext(String id) {
            lastOperationTime = System.currentTimeMillis();

            return contexts.computeIfAbsent(id, _id -> new DebugExecutionContext(id, new Slf4jLogger("rule.engine.debug.none"), this));
        }

        private void execute(RuleData ruleData) {
            String instanceId = ruleData.getAttribute("instanceId").map(String::valueOf).orElse(null);

            RuleInstanceContext context = instanceContext.get(instanceId);
            if (context != null) {
                doExecute(context, ruleData);
            }

        }

        private void doExecute(RuleInstanceContext context, RuleData ruleData) {

            context.execute(Mono.just(ruleData))
                .doOnError((throwable) -> {
                    writeMessage(DebugMessage.of("error", context.getId(), "执行规则失败:" + StringUtils.throwable2String(throwable)));
                })
                .subscribe(resp -> {
                    writeMessage(DebugMessage.of("output", context.getId(), resp.getData()));
                });

        }

        private void execute(ExecuteRuleRequest request) {

            RuleInstanceContext context = instanceContext.get(request.getContextId());
            if (context == null) {
                return;
            }
            RuleData ruleData = RuleData.create(request.getData());
            RuleDataHelper.markStartWith(ruleData, request.getStartWith());
            RuleDataHelper.markSyncReturn(ruleData, request.getEndWith());
            ruleData.setAttribute("debugSessionId", id);
            ruleData.setAttribute("instanceId", request.getContextId());

            doExecute(context, ruleData);
        }

        private Mono<Boolean> writeMessage(DebugMessage message) {
            lastOperationTime = System.currentTimeMillis();
            return Mono.fromRunnable(() -> messageQueue.onNext(message))
                .thenReturn(true);
        }


        @SneakyThrows
        public Flux<DebugMessage> consumeOutPut() {

            return messageQueue
                .map(Function.identity());
        }

        public void close() {
            contexts.forEach((s, context) -> context.stop());
            instanceContext.values().forEach(RuleInstanceContext::stop);
            instanceContext.clear();
            instanceContextMapping.clear();
            messageQueue.onComplete();
        }

    }

    private class DebugExecutionContext implements ExecutionContext {

        private Session session;

        private String id;

        private EmitterProcessor<RuleData> inputQueue = EmitterProcessor.create(false);

        private Logger logger;

        private List<Runnable> stopListener = new CopyOnWriteArrayList<>();

        private long lastOperationTime = System.currentTimeMillis();

        @Getter
        private boolean local = false;

        public DebugExecutionContext(String id, Logger logger, Session session) {
            this.session = session;
            this.logger = logger;
            this.id = id;
        }

        public boolean isTimeout() {
            return System.currentTimeMillis() - lastOperationTime > TimeUnit.MINUTES.toMillis(15);
        }

        @Override
        public String getInstanceId() {
            return id;
        }

        @Override
        public String getNodeId() {
            return id;
        }

        @Override
        public Logger logger() {
            return logger;
        }

        public void execute(RuleData ruleData) {
            lastOperationTime = System.currentTimeMillis();

            ruleData.setAttribute("debug", true);
            inputQueue.onNext(ruleData);
        }

        @Override
        public Input getInput() {

            return new Input() {
                @Override
                public Flux<RuleData> subscribe() {
                    return inputQueue.map(Function.identity())
                        .doOnNext(data -> {
                            log.debug("handle input :{}", data);
                        })
                        .doFinally(s -> {
                            log.debug("unsubscribe input:{}", id);
                        });
                }

                @Override
                public void close() {
                    inputQueue.onComplete();
                }
            };
        }

        @Override
        public Output getOutput() {
            return (data) -> Flux.from(data)
                .flatMap(d -> session.writeMessage(DebugMessage.of("output", id, JSON.toJSONString(d.getData(), SerializerFeature.PrettyFormat))))
                .then(Mono.just(true));
        }

        @Override
        public Mono<Void> fireEvent(String event, RuleData data) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> onError(RuleData data, Throwable e) {
            return session
                .writeMessage(DebugMessage.of("error", id, StringUtils.throwable2String(e)))
                .then();
        }

        @Override
        public void stop() {
            stopListener.forEach(Runnable::run);
        }

        @Override
        public void onStop(Runnable runnable) {
            stopListener.add(runnable);
        }
    }


}
