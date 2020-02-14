package org.jetlinks.community.network.tcp.node;//package org.jetlinks.community.network.tcp.node;
//
//import lombok.AllArgsConstructor;
//import org.jetlinks.community.network.PubSubType;
//import org.jetlinks.community.network.tcp.TcpMessage;
//import org.jetlinks.community.network.tcp.server.TcpServer;
//import org.jetlinks.community.network.tcp.server.TcpServerManager;
//import org.jetlinks.rule.engine.api.RuleData;
//import org.jetlinks.rule.engine.api.RuleDataCodecs;
//import org.jetlinks.rule.engine.api.executor.ExecutionContext;
//import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
//import org.reactivestreams.Publisher;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.util.function.Function;
//
//@AllArgsConstructor
//public class TcpServerNode extends CommonExecutableRuleNodeFactoryStrategy<TcpServerNodeConfig> {
//
//    private TcpServerManager clientManager;
//
//    static {
//        TcpMessageCodec.register();
//    }
//
//    @Override
//    public Function<RuleData, ? extends Publisher<?>> createExecutor(ExecutionContext context, TcpServerNodeConfig config) {
//
//        if (config.getType() != PubSubType.producer) {
//            return Mono::just;
//        }
//        return data -> clientManager.getServer(config.getServerId())
//                .flatMapMany(client -> RuleDataCodecs
//                        .getCodec(TcpMessage.class)
//                        .map(codec -> codec.decode(data, config.getSendPayloadType())
//                                .cast(TcpMessage.class)
//                                .switchIfEmpty(Mono.fromRunnable(() -> context.logger().warn("can not decode rule data to tcp server message:{}", data))))
//                        .orElseGet(() -> Flux.just(new TcpMessage(config.getSendPayloadType().write(data.getData()))))
//                        .flatMap(msg -> client.publish(msg))
//                        .then(Mono.just(data)));
//    }
//
//    @Override
//    protected void onStarted(ExecutionContext context, TcpServerNodeConfig config) {
//        super.onStarted(context, config);
//        if (config.getType() == PubSubType.consumer) {
//            context.onStop(clientManager
//                    .getServer(config.getServerId())
//                    .switchIfEmpty(Mono.fromRunnable(() -> context.logger().error("tcp server {} not found", config.getServerId())))
//                    .flatMapMany(TcpServer::subscribe)
//                    .doOnNext(msg -> context.logger().info("received tcp server message:{}", config.getSubPayloadType().read(msg.getPayload())))
//                    .map(r -> RuleDataCodecs.getCodec(TcpMessage.class)
//                            .map(codec -> codec.encode(r, config.getSubPayloadType()))
//                            .orElse(r.getPayload()))
//                    .onErrorContinue((err, obj) -> {
//                        context.logger().error("consume tcp server message error", err);
//                    })
//                    .subscribe(msg -> context.getOutput().write(Mono.just(RuleData.create(msg))).subscribe())::dispose);
//        }
//    }
//
//    @Override
//    public String getSupportType() {
//        return "tcp-server";
//    }
//}
