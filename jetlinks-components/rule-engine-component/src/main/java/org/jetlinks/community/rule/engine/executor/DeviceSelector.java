package org.jetlinks.community.rule.engine.executor;

import org.jetlinks.core.device.DeviceOperator;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 设备选择器,根据上下文来选择设备
 *
 * @author zhouhao
 * @since 1.5
 */
public interface DeviceSelector {

    /**
     * 根据上下文选择设备
     *
     * @param context 上下文数据
     * @return 设备列表
     */
    Flux<DeviceOperator> select(Map<String, Object> context);

}
