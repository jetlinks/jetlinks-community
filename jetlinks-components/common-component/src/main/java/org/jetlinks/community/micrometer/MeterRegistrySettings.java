package org.jetlinks.community.micrometer;

import org.jetlinks.core.metadata.DataType;

import javax.annotation.Nonnull;

/**
 * 指标注册配置信息
 *
 * @author zhouhao
 * @since 1.11
 */
public interface MeterRegistrySettings {

    /**
     * 给指标添加标签,用于自定义标签类型.在相应指标实现中会根据类型对数据进行存储
     *
     * @param tag  标签key
     * @param type 类型
     */
    void addTag(@Nonnull String tag, @Nonnull DataType type);

}
