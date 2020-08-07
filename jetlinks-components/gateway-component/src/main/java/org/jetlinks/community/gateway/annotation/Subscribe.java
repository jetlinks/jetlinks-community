package org.jetlinks.community.gateway.annotation;

import org.jetlinks.core.event.Subscription;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 订阅来自消息网关的消息
 *
 * @author zhouhao
 * @see org.jetlinks.community.gateway.MessageSubscriber
 * @since 1.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Subscribe {

    @AliasFor("value")
    String[] topics() default {};

    @AliasFor("topics")
    String[] value() default {};

    String id() default "";

    Subscription.Feature[] features() default Subscription.Feature.local;

}
