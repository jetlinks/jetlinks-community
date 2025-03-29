package org.jetlinks.community.device.message;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.DeviceMessage;
import org.springframework.core.Ordered;
import reactor.core.publisher.Mono;

/**
 * 设备消息监听器,在消息被推送到事件总线时执行,用于自定义在推送前到处理逻辑
 *
 * @author zhouhao
 * @since 1.12
 */
public interface DeviceMessagePublishInterceptor extends Ordered {

    /**
     * 判断是否支持处理.如果返回false,则不会调用{@link DeviceMessagePublishInterceptor#beforePublish(DeviceOperator, DeviceMessage)}
     *
     * @param message 消息
     * @return 是否支持
     */
    default boolean isSupport(DeviceMessage message) {
        return true;
    }

    /**
     * 在推送到事件总线前调用
     *
     * @param device  设备
     * @param message 消息
     * @return 新的消息
     */
    Mono<DeviceMessage> beforePublish(DeviceOperator device, DeviceMessage message);

    @Override
   default int getOrder(){
        return Ordered.LOWEST_PRECEDENCE;
    }
}
