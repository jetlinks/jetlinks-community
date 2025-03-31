package org.jetlinks.community.lock;

/**
 * 响应式锁管理器
 *
 * @author zhouhao
 * @see ReactiveLock
 * @since 2.2
 */
public interface ReactiveLockManager {

    /**
     * 根据名称获取一个锁.
     *
     * @param name 锁名称
     * @return 锁
     */
    ReactiveLock getLock(String name);

}
