package org.jetlinks.community.network.tcp.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.PubSubType;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Component
public class TcpClientTaskExecutorProvider implements TaskExecutorProvider {

    private final NetworkManager clientManager;

    static {
        TcpMessageCodec.register();
    }

    @Override
    public String getExecutor() {
        return "tcp-client";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new TcpTaskExecutor(context));
    }

    class TcpTaskExecutor extends AbstractTaskExecutor {

        private TcpClientTaskConfiguration config;

        public TcpTaskExecutor(ExecutionContext context) {
            super(context);
            reload();
        }

        @Override
        public String getName() {
            return "Tcp Client";
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new TcpClientTaskConfiguration());
            config.validate();
        }

        @Override
        public void validate() {
            FastBeanCopier
                .copy(context.getJob().getConfiguration(), new TcpClientTaskConfiguration())
                .validate();
        }

        @Override
        protected Disposable doStart() {
            Disposable.Composite disposable = Disposables.composite();

            if (config.getType() == PubSubType.producer) {
                disposable.add(context
                    .getInput()
                    .accept()
                    .flatMap(data ->
                        clientManager.<TcpClient>getNetwork(DefaultNetworkType.TCP_CLIENT, config.getClientId())
                            .flatMapMany(client -> RuleDataCodecs
                                .getCodec(TcpMessage.class)
                                .map(codec -> codec.decode(data, config.getPayloadType())
                                    .cast(TcpMessage.class)
                                    .switchIfEmpty(Mono.fromRunnable(() -> context.getLogger().warn("can not decode rule data to tcp message:{}", data))))
                                .orElseGet(() -> Flux.just(new TcpMessage(config.getPayloadType().write(data.getData()))))
                                .flatMap(client::send)
                                .onErrorContinue((err, r) -> {
                                    context.onError(err, data).subscribe();
                                })
                                .then()
                            )).subscribe()
                )
                ;
            }
            if (config.getType() == PubSubType.consumer) {
                disposable.add(clientManager.<TcpClient>getNetwork(DefaultNetworkType.TCP_CLIENT, config.getClientId())
                    .switchIfEmpty(Mono.fromRunnable(() -> context.getLogger().error("tcp client {} not found", config.getClientId())))
                    .flatMapMany(TcpClient::subscribe)
                    .doOnNext(msg -> context.getLogger().info("received tcp client message:{}", config.getPayloadType().read(msg.getPayload())))
                    .map(r -> RuleDataCodecs.getCodec(TcpMessage.class)
                        .map(codec -> codec.encode(r, config.getPayloadType()))
                        .orElse(r.getPayload()))
                    .flatMap(out -> context.getOutput().write(Mono.just(RuleData.create(out))))
                    .onErrorContinue((err, obj) -> context.getLogger().error("consume tcp message error", err))
                    .subscribe());
            }
            return disposable;
        }
    }
}
