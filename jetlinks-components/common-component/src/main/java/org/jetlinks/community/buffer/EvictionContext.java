package org.jetlinks.community.buffer;

/**
 * 缓冲淘汰上下文
 *
 * @author zhouhao
 * @since 2.0
 */
public interface EvictionContext {

    /**
     * 获取指定类型的数据量
     *
     * @param type 类型
     * @return 数据量
     */
    long size(BufferType type);

    /**
     * 删除最新的数据
     *
     * @param type 类型
     */
    void removeLatest(BufferType type);

    /**
     * 删除最旧的数据
     *
     * @param type 类型
     */
    void removeOldest(BufferType type);

    /**
     * @return 缓冲区名称, 用于区分多个不同的缓冲区
     */
    String getName();

    enum BufferType {
        //缓冲区
        buffer,
        //死数据
        dead
    }
}
