package org.jetlinks.community.standalone.configuration;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JetLinksProperties.class)
@Slf4j
public class JetLinksConfiguration {

    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> webServerFactoryWebServerFactoryCustomizer() {
        //解决请求参数最大长度问题
        return factory -> factory.addServerCustomizers(httpServer ->
            httpServer.httpRequestDecoder(spec -> {
                spec.maxInitialLineLength(10240);
                return spec;
            }));
    }

    @Bean
    @ConfigurationProperties(prefix = "vertx")
    public VertxOptions vertxOptions() {
        return new VertxOptions();
    }

    @Bean
    public Vertx vertx(VertxOptions vertxOptions) {
        return Vertx.vertx(vertxOptions);
    }


}
