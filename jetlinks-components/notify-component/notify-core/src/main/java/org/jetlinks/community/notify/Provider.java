package org.jetlinks.community.notify;

/**
 * 服务商标识,通常使用枚举实现此接口
 *
 * @author zhouhao
 * @since 1.0
 */
public interface Provider {
    /**
     * @return 唯一标识
     */
    String getId();

    /**
     * @return 名称
     */
    String getName();

}
