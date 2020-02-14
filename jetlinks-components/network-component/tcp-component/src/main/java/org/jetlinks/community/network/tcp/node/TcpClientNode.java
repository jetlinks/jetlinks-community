package org.jetlinks.community.network.tcp.node;

import lombok.AllArgsConstructor;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.PubSubType;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@AllArgsConstructor
public class TcpClientNode extends CommonExecutableRuleNodeFactoryStrategy<TcpClientNodeConfig> {

    private NetworkManager clientManager;

    static {
        TcpMessageCodec.register();
    }

    @Override
    public Function<RuleData, ? extends Publisher<?>> createExecutor(ExecutionContext context, TcpClientNodeConfig config) {

        if (config.getType() != PubSubType.producer) {
            return Mono::just;
        }
        return data -> clientManager.<TcpClient>getNetwork(DefaultNetworkType.TCP_CLIENT,config.getClientId())
                .flatMapMany(client -> RuleDataCodecs
                        .getCodec(TcpMessage.class)
                        .map(codec -> codec.decode(data, config.getSendPayloadType())
                                .cast(TcpMessage.class)
                                .switchIfEmpty(Mono.fromRunnable(() -> context.logger().warn("can not decode rule data to tcp message:{}", data))))
                        .orElseGet(() -> Flux.just(new TcpMessage(config.getSendPayloadType().write(data.getData()))))
                        .flatMap(client::send)
                        .all(r-> r))
                        ;
    }

    @Override
    protected void onStarted(ExecutionContext context, TcpClientNodeConfig config) {
        super.onStarted(context, config);
        if (config.getType() == PubSubType.consumer) {
            context.onStop( clientManager.<TcpClient>getNetwork(DefaultNetworkType.TCP_CLIENT,config.getClientId())
                    .switchIfEmpty(Mono.fromRunnable(() -> context.logger().error("tcp client {} not found", config.getClientId())))
                    .flatMapMany(TcpClient::subscribe)
                    .doOnNext(msg -> context.logger().info("received tcp client message:{}", config.getSubPayloadType().read(msg.getPayload())))
                    .map(r -> RuleDataCodecs.getCodec(TcpMessage.class)
                            .map(codec -> codec.encode(r, config.getSubPayloadType()))
                            .orElse(r.getPayload()))
                    .onErrorContinue((err, obj) -> {
                        context.logger().error("consume tcp message error", err);
                    })
                    .subscribe(msg -> context.getOutput().write(Mono.just(RuleData.create(msg))).subscribe())::dispose);
        }
    }

    @Override
    public String getSupportType() {
        return "tcp-client";
    }
}
