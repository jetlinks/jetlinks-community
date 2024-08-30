package org.jetlinks.community.buffer;

/**
 * 已缓冲的数据
 *
 * @param <T> 数据类型
 * @author zhouhao
 * @since 2.2
 */
public interface Buffered<T> {

    /**
     * @return 数据
     */
    T getData();

    /**
     * @return 当前重试次数
     */
    int getRetryTimes();

    /**
     * 标记是否重试此数据
     */
    void retry(boolean retry);

    /**
     * 标记此数据为死信
     */
    void dead();
}
