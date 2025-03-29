package org.jetlinks.community.device.message;

import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.Values;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.defaults.DeviceThingsRegistrySupport;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.DeviceThingType;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.message.*;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.core.things.Thing;
import org.jetlinks.core.things.ThingType;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.trace.TraceHolder;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.things.ThingConstants;
import org.jetlinks.community.utils.TopicUtils;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.Function;

/**
 * 主要功能:
 * <pre>
 *     1. 实现{@link DecodedClientMessageHandler}统一处理由设备网关解码后的设备消息,将消息转发到事件总线{@link  EventBus}中.
 *     2. 监听设备会话注册注销，生成设备上线离线消息,转发到事件总线中.
 * </pre>
 * <p>
 * 核心方法:
 * {@link  DeviceMessageConnector#onMessage(Message)} ,
 * {@link DeviceMessageConnector#handleMessage(DeviceOperator, Message)}
 *
 * @author zhouhao
 * @see DecodedClientMessageHandler
 * @see TopicUtils
 * @see DeviceMessageConnector#onMessage(Message)
 * @since 1.0
 */
//todo 迁移此功能到things-component,统一处理ThingMessage
@Slf4j
public class DeviceMessageConnector implements DecodedClientMessageHandler, SmartInitializingSingleton, ApplicationContextAware {

    private static final AtomicIntegerFieldUpdater<DeviceMessageConnector>
        SHUTDOWN = AtomicIntegerFieldUpdater.newUpdater(DeviceMessageConnector.class, "shutdown");
    private static final AtomicLongFieldUpdater<DeviceMessageConnector>
        REMAINDER = AtomicLongFieldUpdater.newUpdater(DeviceMessageConnector.class, "remainder");

    //将设备注册中心的配置追加到消息header中,下游订阅者可直接使用.
    //这些信息是在设备注册时,自动加载到config中的.
    private final static Collection<String> allConfigHeader = Sets
        .newHashSet(
            //产品ID
            PropertyConstants.productId.getKey(),
            //产品名称
            PropertyConstants.productName.getKey(),
            //设备名称
            PropertyConstants.deviceName.getKey(),
            //创建者ID
            PropertyConstants.creatorId.getKey()
        );

    /**
     * 添加自定义的配置信息到header中
     * 在转发到eventbus之前会获取设备的配置信息,并设置到消息的header中,
     * 下游订阅到消息时,可从header中获取来进行自定义的业务.
     * <p>
     * 注意: 这里只会获取设备自身的配置,不会尝试获取产品的配置.
     *
     * @param key 配置key
     * @see DeviceOperator#getConfig(String)
     * @see Thing#getConfig(String)
     */
    public synchronized static void addConfigHeaderKey(String key) {
        allConfigHeader.add(key);
    }

    /**
     * @see DeviceMessageConnector#addConfigHeaderKey(String)
     */
    public synchronized static void addConfigHeaderKey(ConfigKey<?> key) {
        allConfigHeader.add(key.getKey());
    }

    private volatile int shutdown = 0;

    private volatile long remainder = 0;

    //设备注册中心
    private final ThingsRegistry registry;

    //事件总线
    private final EventBus eventBus;

    //消息处理器,用于将设备回复的指令返回给指令发起者
    private final MessageHandler messageHandler;

    //消息拦截器,用于在消息推送到事件总线之前进行自定义操作,如果添加自定义header等操作
    private final CompositeDeviceMessagePublishInterceptor interceptor = new CompositeDeviceMessagePublishInterceptor();

    //错误监听器,定义全局错误处理
    private final static Function<Throwable, Mono<Void>> doOnError = (error) -> {
        DeviceMessageConnector.log.error(error.getMessage(), error);
        return Mono.empty();
    };

    //空配置定义
    private final static Values emptyValues = Values.of(Collections.emptyMap());

    public DeviceMessageConnector(EventBus eventBus,
                                  ThingsRegistry registry,
                                  MessageHandler messageHandler) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.messageHandler = messageHandler;

    }

    public DeviceMessageConnector(EventBus eventBus,
                                  DeviceRegistry registry,
                                  MessageHandler messageHandler) {
        this(eventBus, new DeviceThingsRegistrySupport(registry), messageHandler);
    }

    private String createUid() {
        return IDGenerator.RANDOM.generate();
    }

    public Mono<Void> onMessage(Message message) {
        //标记来忽略处理的消息则忽略处理
        if (null == message || message.getHeader(Headers.ignore).orElse(false)) {
            return Mono.empty();
        }
        //添加消息ID,下游可以使用此ID进行去重
        //场景: 一个消息,可能会被推送到多个topic,比如: 有租户,机构等绑定信息时.
        //     如果用户同时在多个租户(A,B)里,设备也同时在这些租户里,那用户可能会收到多次推送(租户A一次,租户B一次)
        //     这时可以利用uid进行去重,参照: ReactorUtils.distinct
        message.getOrAddHeader(PropertyConstants.uid, this::createUid);

        //处理设备消息
        if (message instanceof DeviceMessage) {
            return handleDeviceMessage(((DeviceMessage) message));
        }
        //处理非设备消息
        if (message instanceof ThingMessage) {
            return handleThingMessage(((ThingMessage) message));
        }
        //其他消息暂不支持
        return Mono.empty();
    }

    private Mono<Void> handleDeviceMessage(DeviceMessage message) {
        REMAINDER.incrementAndGet(this);
        return this
            //转化为设备消息
            .convertToDeviceMessage(message)
            .flatMap(msg -> this
                .getTopic(msg)
                //推送到事件总线中
                .flatMap(topic -> eventBus.publish(topic, msg))
                .then())
            .doAfterTerminate(() -> REMAINDER.decrementAndGet(this))
            ;
    }

    private Mono<Void> handleThingMessage(ThingMessage message) {
        REMAINDER.incrementAndGet(this);
        return registry
            .getThing(message.getThingType(), message.getThingId())
            .flatMap(thing -> Mono
                .zip(
                    refactorHeader(thing, message),//重构header
                    thing.getTemplate(),//获取模版,如模版不存在,将不会推送数据.
                    (_msg, template) -> {
                        ThingMessage msg = (ThingMessage) _msg;
                        msg.addHeader(ThingConstants.templateId, template.getId());
                        ThingType type = ThingType.of(msg.getThingType());
                        String topic = TopicUtils.createMessageTopic(msg, type.getTopicPrefix(template.getId(), msg.getThingId()));
                        return Flux
                            .fromIterable(TopicUtils.refactorTopic(msg, topic))
                            .flatMap(_topic -> eventBus.publish(_topic, msg))
                            .then();
                    })
                .flatMap(Function.identity()))
            .doAfterTerminate(() -> REMAINDER.decrementAndGet(this));
    }

    protected Mono<DeviceMessage> convertToDeviceMessage(Message message) {
        //只处理设备消息,其他忽略
        if (message instanceof DeviceMessage) {
            DeviceMessage msg = ((DeviceMessage) message);
            String deviceId = msg.getDeviceId();
            return registry
                .getThing(DeviceThingType.device, deviceId)
                //重构消息的header,向header中添加一些常用信息,下游可直接从header中获取使用
                //  而不用再从注册中心里获取
                .flatMap(device -> refactorHeader(device, msg)
                    //执行拦截器
                    .flatMap(newMsg -> interceptor.beforePublish(device.unwrap(DeviceOperator.class), (DeviceMessage) newMsg)))
                .switchIfEmpty(
                    //设备注册直接返回,否则可能无法自动注册
                    msg instanceof DeviceRegisterMessage ? Mono.just(msg)
                        //其他消息抛出错误,便于上游重试等操作.
                        : Mono.error(() -> new NotFoundException.NoStackTrace("error.device_not_found", deviceId)));
        }
        return Mono.empty();
    }

    private Flux<SeparatedCharSequence> getTopic(DeviceMessage message) {
        //构造topic
        Flux<SeparatedCharSequence> topicsStream = Flux.fromIterable(createDeviceMessageTopic(message));
        if (message instanceof ChildDeviceMessage) { //子设备消息
            if (((ChildDeviceMessage) message).getChildDeviceMessage() != null) {
                return this
                    //子设备消息也进行转发
                    .onMessage(((ChildDeviceMessage) message)
                                   .getChildDeviceMessage()
                                   .addHeader(DeviceConfigKey.parentGatewayId.getKey(), message.getDeviceId()))
                    .thenMany(topicsStream);
            }
        } else if (message instanceof ChildDeviceMessageReply) { //子设备消息回复
            if (((ChildDeviceMessageReply) message).getChildDeviceMessage() != null) {
                return this
                    //子设备消息也进行转发
                    .onMessage(((ChildDeviceMessageReply) message)
                                   .getChildDeviceMessage()
                                   .addHeader(DeviceConfigKey.parentGatewayId.getKey(), message.getDeviceId()))
                    .thenMany(topicsStream);
            }
        }
        return topicsStream;
    }

    private static final SharedPathString devicePrefix = SharedPathString.of("/device/*/*");

    static Set<SeparatedCharSequence> createDeviceMessageTopic(DeviceMessage msg) {

        String deviceId = msg.getDeviceId();
        //从header中获取产品ID,
        String productId = msg.getHeaderOrElse(PropertyConstants.productId, () -> "null");

//        //使用StringBuilderUtils来快速构造topic
//        String topic = StringBuilderUtils
//            .buildString(deviceMessage, (msg, builder) -> {
//                String deviceId = msg.getDeviceId();
//                //从header中获取产品ID,
//                String productId = msg.getHeaderOrElse(PropertyConstants.productId, () -> "null");
//                //topic前缀: /device/{productId}/{deviceId}
//                builder.append("/device/")
//                       .append(productId)
//                       .append('/')
//                       .append(deviceId);
//                //根据消息拼接topic
//                TopicUtils.appendMessageTopic(msg, builder);
//            });

        SeparatedCharSequence prefix = devicePrefix.replace(2, productId, 3, deviceId);
        SeparatedCharSequence origin = TopicUtils.createMessageTopic(msg,prefix);

        return TopicUtils.refactorTopic(msg, origin);
    }

    static Mono<Message> refactorHeader(Thing thing, Message message) {
        if (message instanceof ThingMessage) {
            ThingMessage thingMessage = ((ThingMessage) message);
            String thingId = thingMessage.getThingId();
            if (!StringUtils.hasText(thingId)) {
                log.warn("无法从消息中获取ID:{}", thingMessage);
                return Mono.just(message);
            }
            return thing
                .getSelfConfigs(allConfigHeader)
                .defaultIfEmpty(emptyValues)
                .map(configs -> {
                    configs.getAllValues().forEach(thingMessage::addHeader);
                    return thingMessage;
                });
        }
        return Mono.just(message);
    }

    public static Flux<SeparatedCharSequence> createDeviceMessageTopic(DeviceRegistry registry, Message message) {
        if (message instanceof DeviceMessage) {
            DeviceMessage deviceMessage = ((DeviceMessage) message);
            String deviceId = deviceMessage.getDeviceId();
            if (!StringUtils.hasText(deviceId)) {
                log.warn("无法从消息中获取设备ID:{}", deviceMessage);
                return Flux.empty();
            }
            return registry
                .getDevice(deviceId)
                .flatMapMany(device -> refactorHeader(device, deviceMessage)
                    .cast(DeviceMessage.class)
                    .flatMapIterable(DeviceMessageConnector::createDeviceMessageTopic));
        }
        return Flux.empty();
    }

    protected Mono<Boolean> handleChildrenDeviceMessage(Message message) {
        //消息回复,则返回给指令的发起者
        if (message instanceof DeviceMessageReply) {
            return doReply(((DeviceMessageReply) message));
        }
        //不处理子设备上下线,统一由 DeviceGatewayHelper处理
        return Reactors.ALWAYS_TRUE;
    }

    @Override
    public Mono<Boolean> handleMessage(DeviceOperator device, @Nonnull Message message) {
        //已经停止服务,返回false.便于上游处理重试等操作.
        if (isShutdown()) {
            return Reactors.ALWAYS_FALSE;
        }
        Mono<Boolean> then;
        //子设备的消息回复
        if (message instanceof ChildDeviceMessageReply) {
            then = this
                //回复给网关设备
                .doReply(((ChildDeviceMessageReply) message))
                .then(
                    handleChildrenDeviceMessage(((ChildDeviceMessageReply) message).getChildDeviceMessage())
                );
        }
        //子设备消息
        else if (message instanceof ChildDeviceMessage) {
            then = handleChildrenDeviceMessage(((ChildDeviceMessage) message).getChildDeviceMessage());
        }
        //设备回复消息
        else if (message instanceof DeviceMessageReply) {
            then = doReply(((DeviceMessageReply) message));
        }
        //其他消息
        else {
            //do nothing
            then = Reactors.ALWAYS_TRUE;
        }
        return TraceHolder
            .writeContextTo(message, Message::addHeader)
            .flatMap(this::onMessage)
            .then(then)
            .defaultIfEmpty(false);

    }

    private Mono<Boolean> doReply(DeviceMessageReply reply) {
        log.debug("reply message {}", reply.getMessageId());
        //交给处理器统一处理
        return messageHandler
            .reply(reply)
            .thenReturn(true)
            .doOnError((error) -> log.error("reply message error", error))
            .as(MonoTracer.create(DeviceTracer.SpanName.response(reply.getDeviceId())
                , builder -> builder.setAttributeLazy(DeviceTracer.SpanKey.message, reply::toString)))
            .as(MonoTracer.createWith(reply.getHeaders()))
            ;
    }

    public boolean isShutdown() {
        return SHUTDOWN.get(this) == -1;
    }

    @SneakyThrows
    public void shutdown() {

        SHUTDOWN.set(this, -1);
        int count = 16;
        long remainder = REMAINDER.get(this);
        log.info("shutdown device message connector,remainder:{}", remainder);
        do {
            if (remainder > 0) {
                log.info("wait remainder message: {}", remainder);
            }
            Thread.sleep(300);
            remainder = REMAINDER.get(this);
        }
        while (remainder > 0 && count-- > 0);

    }

    @Override
    public void afterSingletonsInstantiated() {
        if (applicationContext != null) {
            applicationContext
                .getBeansOfType(DeviceMessagePublishInterceptor.class)
                .values()
                .forEach(interceptor::addInterceptor);
        }
    }

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
