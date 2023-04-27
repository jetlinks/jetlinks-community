package org.jetlinks.community.rule.engine.executor;

import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;

/**
 * 设备选择器构造器,根据表达式来构造一个选择器.
 *
 * @author zhouhao
 * @see DeviceSelector
 * @since 1.5
 */
public interface DeviceSelectorBuilder {


    /**
     * 根据选择器描述来创建选择器
     *
     * @param spec 描述
     * @return 选择器
     * @see org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProvider
     */
    DeviceSelector createSelector(DeviceSelectorSpec spec);
}
