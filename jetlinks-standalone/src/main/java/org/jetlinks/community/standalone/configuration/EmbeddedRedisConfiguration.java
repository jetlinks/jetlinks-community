package org.jetlinks.community.standalone.configuration;

import com.github.tonivade.claudb.ClauDB;
import com.github.tonivade.claudb.DBConfig;
import com.github.tonivade.resp.RespServer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import java.io.File;

public class EmbeddedRedisConfiguration implements ApplicationListener<ApplicationPreparedEvent>, Ordered {

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        Environment environment = event.getApplicationContext().getEnvironment();
        if (!environment.getProperty("spring.redis.embedded.enabled", Boolean.class, false)) {
            return;
        }

        String dataPath = environment.getProperty("spring.redis.embedded.data-path", "./data/redis");
        new File(dataPath).mkdirs();
        DBConfig config = new DBConfig();
        config.setPersistenceActive(true);
        config.setAofFile(dataPath.concat("/jetlinks.aof"));
        config.setRdbFile(dataPath.concat("/jetlinks.rdb"));

        RespServer server = ClauDB.builder()
            .port(environment.getProperty("spring.redis.embedded.port", Integer.class, 6379))
            .host(environment.getProperty("spring.redis.embedded.host", "0.0.0.0"))
            .config(config)
            .build();
        server.start();

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}