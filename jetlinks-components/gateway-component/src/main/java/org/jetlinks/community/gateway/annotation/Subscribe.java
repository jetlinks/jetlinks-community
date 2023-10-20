package org.jetlinks.community.gateway.annotation;

import org.jetlinks.core.event.Subscription;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 从事件总线{@link org.jetlinks.core.event.EventBus}中订阅消息并执行注解的方法,
 * 事件总线的输出数据可以作为方法参数,如果类型不一致会自动转换。
 * 也可以通过方法参数直接获取事件总线的原始数据:{@link org.jetlinks.core.event.TopicPayload}
 *
 * <pre>
 * &#64;Subscribe("/device/&#42;/&#42;/message")
 * public Mono&lt;Void&gt; handleEvent(DeviceMessage msg){
 *      return doSomeThing(msg);
 * }
 * </pre>
 *
 * @author zhouhao
 * @see org.jetlinks.core.event.EventBus
 * @see org.jetlinks.community.gateway.spring.SpringMessageBroker
 * @since 1.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Subscribe {

    /**
     * 要订阅的topic,topic是树结构,
     * 和{@link org.springframework.util.AntPathMatcher}类似,支持通配符: **表示多层目录,*表示单层目录.
     * <ul>
     *     <li>
     *        /device/p1/d1/online
     *     </li>
     *     <li>
     *       /device/p1/d1,d2/online
     *     </li>
     *     <li>
     *        /device/p1/&#42;/online
     *     </li>
     *     <li>
     *       /device/&#42;&#42;
     *    </li>
     * </ul>
     * <p>
     * 支持使用表达式
     * <pre>
     * /device/${sub.product-id}/**
     * </pre>
     *
     * @return topics
     * @see Subscribe#value()
     * @see org.jetlinks.core.event.EventBus#subscribe(Subscription)
     */
    @AliasFor("value")
    String[] topics() default {};

    /**
     * @return topics
     * @see Subscribe#topics()
     */
    @AliasFor("topics")
    String[] value() default {};

    /**
     * 指定订阅者ID,默认为方法名
     *
     * @return 订阅者ID
     */
    String id() default "";

    /**
     * 订阅特性，默认只订阅本地进程内部的消息
     *
     * @return 订阅特性
     */
    Subscription.Feature[] features() default Subscription.Feature.local;

    /**
     *
     * @return 订阅优先级,值越小优先级越高.
     */
    int priority() default Integer.MAX_VALUE;
}
