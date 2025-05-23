/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
