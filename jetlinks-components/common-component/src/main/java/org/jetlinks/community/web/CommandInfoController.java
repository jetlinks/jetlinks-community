package org.jetlinks.community.web;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.community.command.CommandSupportManagerProvider;
import org.jetlinks.community.command.CommandSupportManagerProviders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 获取平台内部命令信息接口
 *
 * @author zhouhao
 * @since 2.2
 */
@RestController
@RequestMapping("/command-supports")
@Hidden
@AllArgsConstructor
public class CommandInfoController {

    @GetMapping("/services")
    @SneakyThrows
    @Authorize
    public Flux<CommandSupportManagerProvider.CommandSupportInfo> getServices() {
        return Flux
            .fromIterable(CommandSupportManagerProvider.supports.getAll())
            .flatMap(provider -> provider
                .getSupportInfo()
                .map(s -> s.copy().appendService(provider.getProvider()))
                .defaultIfEmpty(
                    CommandSupportManagerProvider
                        .CommandSupportInfo
                        .of(provider.getProvider(), null, null)));
    }

    @GetMapping("/service/{serviceId}/commands")
    @SneakyThrows
    @Authorize
    public Flux<FunctionMetadata> getServiceCommands(@PathVariable String serviceId) {
        return CommandSupportManagerProviders
            .getCommandSupport(serviceId)
            .flatMapMany(CommandSupport::getCommandMetadata);
    }

    @GetMapping("/service/{serviceId}/exists")
    @SneakyThrows
    @Authorize
    public Mono<Boolean> getServiceCommandSupport(@PathVariable String serviceId) {
        return CommandSupportManagerProviders
            .getCommandSupport(serviceId)
            .hasElement()
            .onErrorResume(err -> Mono.just(false));
    }
}
