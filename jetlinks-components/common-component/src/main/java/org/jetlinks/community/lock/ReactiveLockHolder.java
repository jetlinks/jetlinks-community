package org.jetlinks.community.lock;

/**
 * 响应式锁持有器,用于通过静态方法获取锁.
 * <pre>{@code
 *
 *  Mono<MyEntity> execute(MyEntity entity){
 *
 *  return ReactiveLockHolder
 *          .getLock("lock-test:"+entity.getId())
 *          .lock(updateAndGet(entity));
 *
 * }
 * }</pre>
 *
 * @author zhouhao
 * @see ReactiveLock
 * @see ReactiveLockManager
 * @since 2.2
 */
public class ReactiveLockHolder {

    private static ReactiveLockManager lockManager = new DefaultReactiveLockManager();

    static void setup(ReactiveLockManager manager) {
        lockManager = manager;
    }

    /**
     * 根据锁名称获取锁
     *
     * @param name 锁名称
     * @return 锁
     */
    public static ReactiveLock getLock(String name) {
        return lockManager.getLock(name);
    }

}
