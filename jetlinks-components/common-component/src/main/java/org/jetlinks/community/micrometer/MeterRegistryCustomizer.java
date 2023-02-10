package org.jetlinks.community.micrometer;

/**
 * 监控指标自定义注册接口,用于对指标进行自定义,如添加指标标签等操作
 *
 * @author zhouhao
 * @since 1.11
 */
public interface MeterRegistryCustomizer {

    /**
     * 在指标首次初始化时调用,可以通过判断metric进行自定义标签
     *
     * @param metric   指标
     * @param settings 自定义设置
     */
    void custom(String metric, MeterRegistrySettings settings);

}
