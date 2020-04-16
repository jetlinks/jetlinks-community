package org.jetlinks.community.gateway.external.socket;

import org.hswebframework.web.authorization.ReactiveAuthenticationManager;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.jetlinks.community.gateway.external.MessagingManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
//@ConditionalOnBean({
//    ReactiveAuthenticationManager.class,
//    UserTokenManager.class
//})
public class WebSocketMessagingHandlerConfiguration {


    @Bean
    public HandlerMapping webSocketMessagingHandlerMapping(MessagingManager messagingManager,
                                           UserTokenManager userTokenManager,
                                           ReactiveAuthenticationManager authenticationManager) {


        WebSocketMessagingHandler messagingHandler=new WebSocketMessagingHandler(
            messagingManager,
            userTokenManager,
            authenticationManager
        );
        final Map<String, WebSocketHandler> map = new HashMap<>(1);
        map.put("/messaging/**", messagingHandler);

        final SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        mapping.setUrlMap(map);
        return mapping;
    }

    @Bean
    @ConditionalOnMissingBean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }


}
