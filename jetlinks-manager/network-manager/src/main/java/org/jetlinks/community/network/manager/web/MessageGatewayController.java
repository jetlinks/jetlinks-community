package org.jetlinks.community.network.manager.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.jetlinks.community.network.manager.web.response.TopicMessageResponse;
import org.jetlinks.community.gateway.*;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("gateway/message")
@Resource(id = "message-gateway", name = "消息网关")
@Authorize
public class MessageGatewayController {

    private final MessageGatewayManager messageGatewayManager;

    public MessageGatewayController(MessageGatewayManager gatewayManager) {
        this.messageGatewayManager = gatewayManager;
    }


    @GetMapping(value = "/{id}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResourceAction(id = "subscribe", name = "订阅消息")
    public Flux<TopicMessage> subscribe(@PathVariable String id, @RequestParam String topic) {

        return messageGatewayManager
            .getGateway(id)
            .flatMapMany(gateway -> gateway.subscribe(topic));
    }

    @PostMapping(value = "/{id}/publish")
    @ResourceAction(id = "publish", name = "推送消息")
    public Mono<Long> publish(@PathVariable String id, @RequestBody TopicMessageResponse response) {
        return messageGatewayManager
            .getGateway(id)
            .flatMapMany(gateway -> gateway.publish(response.toTopicMessage()))
            .count();
    }


    @GetMapping(value = "/all")
    @Authorize(merge = false)
    public Flux<GatewayInfo> getAllGateway() {
        return messageGatewayManager
            .getAllGateway()
            .map(GatewayInfo::of);
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GatewayInfo {
        private String id;

        private String name;

        public static GatewayInfo of(MessageGateway gateway) {
            return new GatewayInfo(gateway.getId(), gateway.getName());
        }
    }
}
