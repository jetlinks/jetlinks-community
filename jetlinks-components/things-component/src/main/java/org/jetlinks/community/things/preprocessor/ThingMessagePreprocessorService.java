package org.jetlinks.community.things.preprocessor;

import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.things.ThingMetadata;
import reactor.core.publisher.Mono;

import java.util.Collection;


/**
 * 物消息预处理器,用于在接收到物消息时进行预处理,如: 过滤不符合规则的消息,转换消息格式等.
 *
 * @author zhouhao
 * @since 2.2
 */
public interface ThingMessagePreprocessorService {

    /**
     * 处理模板物模型变更,在产品模型变更时执行此方法,用于同步删除规则等操作.
     *
     * @param thingType  物类型
     * @param templateId 模板ID
     * @param before     变更前的物模型
     * @param after      变更后的物模型
     * @return void
     */
    Mono<Void> handleTemplateMetadataChanged(String thingType,
                                             String templateId,
                                             ThingMetadata before,
                                             ThingMetadata after);

    /**
     * 处理物的物模型变更,在设备物模型变更时执行此方法,用于同步删除规则等操作.
     *
     * @param thingType 物类型
     * @param thingId   物ID
     * @param before    变更前的物模型
     * @param after     变更后的物模型
     * @return void
     */
    Mono<Void> handleThingMetadataChanged(String thingType,
                                          String thingId,
                                          ThingMetadata before,
                                          ThingMetadata after);

    /**
     * 处理物实例删除
     *
     * @param thingType 物类型
     * @param thingId   物ID
     * @return void
     */
    Mono<Void> handleThingsRemoved(String thingType,
                                   Collection<String> thingId);

    /**
     * 处理物模板删除
     *
     * @param thingType 物类型
     * @param template  模板ID
     * @return void
     */
    Mono<Void> handleTemplatesRemoved(String thingType,
                                      Collection<String> template);

    /**
     * 处理物消息,用于对接收到的消息进行二次加工,如: 过滤不符合规则的消息,转换消息格式等.
     *
     * @param message 物消息
     * @return 处理后的消息
     * @see org.jetlinks.core.message.property.PropertyMessage
     * @see org.jetlinks.core.message.property.ThingReportPropertyMessage
     */
    Mono<ThingMessage> handleMessage(ThingMessage message);

}
