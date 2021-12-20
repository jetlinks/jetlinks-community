package org.jetlinks.community.test.spring;

import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@SpringBootApplication(
    scanBasePackages = {
        "org.jetlinks.community.device",
    },
    exclude = {
        DataSourceAutoConfiguration.class,
        KafkaAutoConfiguration.class,
        RabbitAutoConfiguration.class,
//        ElasticsearchRestClientAutoConfiguration.class,
//        ElasticsearchDataAutoConfiguration.class,
    }
)
@EnableEasyormRepository("org.jetlinks.device.**.entity")
public class TestJetLinksApplication {


}
