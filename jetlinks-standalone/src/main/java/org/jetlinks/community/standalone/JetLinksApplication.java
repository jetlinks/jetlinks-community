package org.jetlinks.community.standalone;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.basic.configuration.EnableAopAuthorize;
import org.hswebframework.web.authorization.events.AuthorizingHandleBeforeEvent;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.hswebframework.web.logging.aop.EnableAccessLogger;
import org.hswebframework.web.logging.events.AccessLoggerAfterEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


@SpringBootApplication(scanBasePackages = "org.jetlinks.community", exclude = {
    DataSourceAutoConfiguration.class,
    ElasticsearchRestClientAutoConfiguration.class
})
@EnableCaching
@EnableEasyormRepository("org.jetlinks.community.**.entity")
@EnableAopAuthorize
@EnableAccessLogger
@Slf4j
public class JetLinksApplication {

    public static void main(String[] args) {
        SpringApplication.run(JetLinksApplication.class, args);
    }

    @Profile("dev")
    @Component
    @Slf4j
    public static class AdminAllAccess {

        @PostConstruct
        public void init() {
            // TODO: 2020/1/4 严重影响性能，谨慎开启
            // Hooks.onOperatorDebug();
        }

        @EventListener
        public void handleAuthEvent(AuthorizingHandleBeforeEvent e) {
            //admin用户拥有所有权限
            if (e.getContext().getAuthentication().getUser().getUsername().equals("admin")) {
                e.setAllow(true);
            }
        }

        @EventListener
        public void handleAccessLogger(AccessLoggerAfterEvent event) {

            log.info("{}=>{} {}-{}", event.getLogger().getIp(), event.getLogger().getUrl(), event.getLogger().getDescribe(), event.getLogger().getAction());

        }
    }


}
