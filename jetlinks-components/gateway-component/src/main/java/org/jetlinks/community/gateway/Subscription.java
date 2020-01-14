package org.jetlinks.community.gateway;

import lombok.*;

/**
 * 订阅信息.支持通配符**(匹配多层目录)和*(匹配单层目录).
 *
 * @author zhouhao
 * @since 1.0
 */
@Getter
@Setter
@EqualsAndHashCode(of = "topic")
public class Subscription {

    private String topic;

    public Subscription(String topic) {
        //适配mqtt topic通配符
        if (topic.contains("#") || topic.contains("+")) {
            topic = topic.replace("#", "**").replace("+", "*");
        }
        this.topic = topic;
    }

}
