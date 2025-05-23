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
package org.jetlinks.community.buffer;

import org.springframework.util.unit.DataSize;

import java.io.File;

/**
 * 缓存淘汰策略
 *
 * @author zhouhao
 * @since 2.0
 */
public interface BufferEviction {

    BufferEviction NONE = new BufferEviction() {
        @Override
        public boolean tryEviction(EvictionContext context) {
            return false;
        }

        @Override
        public String toString() {
            return "None";
        }
    };

    /**
     * 根据磁盘使用率来进行淘汰,磁盘使用率超过阈值时则淘汰旧数据
     *
     * @param path      文件路径
     * @param threshold 使用率阈值 范围为0-1 .如: 0.8 表示磁盘使用率超过80%则丢弃数据
     * @return 淘汰策略
     */
    static BufferEviction disk(String path, float threshold) {
        return new DiskUsageEviction(new File(path), threshold);
    }

    /**
     * 根据磁盘可用空间来进行淘汰,磁盘剩余空间低于阈值时则淘汰旧数据
     *
     * @param path              文件路径
     * @param minUsableDataSize 磁盘最小可用空间阈值,当磁盘可用空间低于此值时则则淘汰旧数据
     * @return 淘汰策略
     */
    static BufferEviction disk(String path, DataSize minUsableDataSize) {
        return new DiskFreeEviction(new File(path), minUsableDataSize.toBytes());
    }

    /**
     * 根据缓冲区数量来淘汰数据,当数量超过指定阈值后则淘汰旧数据
     *
     * @param bufferLimit 数量阈值
     * @return 淘汰策略
     */
    static BufferEviction limit(long bufferLimit) {
        return limit(bufferLimit, bufferLimit);
    }

    /**
     * 根据缓冲区数量来淘汰数据,当数量超过指定阈值后则淘汰旧数据
     *
     * @param bufferLimit 缓冲数量阈值
     * @param deadLimit   死数据数量阈值
     * @return 淘汰策略
     */
    static BufferEviction limit(long bufferLimit, long deadLimit) {
        return new SizeLimitEviction(bufferLimit, deadLimit);
    }

    /**
     * 根据缓冲区数量来淘汰死数据
     *
     * @param deadLimit 死数据数量阈值
     * @return 淘汰策略
     */
    static BufferEviction deadLimit(long deadLimit) {
        return new SizeLimitEviction(-1, deadLimit);
    }

    /**
     * 尝试执行淘汰
     *
     * @param context 上下文
     * @return 是否有数据被淘汰
     */
    boolean tryEviction(EvictionContext context);

    /**
     * 组合另外一个淘汰策略,2个策略同时执行.
     *
     * @param after 后续策略
     * @return 淘汰策略
     */
    default BufferEviction and(BufferEviction after) {
        BufferEviction self = this;
        return new BufferEviction() {
            @Override
            public boolean tryEviction(EvictionContext context) {
               return self.tryEviction(context) & after.tryEviction(context);
            }

            @Override
            public String toString() {
                return self + " and " + after;
            }
        };
    }

    /**
     * 组合另外一个淘汰策略,当前策略淘汰了数据才执行另外一个策略
     *
     * @param after 后续策略
     * @return 淘汰策略
     */
    default BufferEviction then(BufferEviction after) {
        BufferEviction self = this;
        return new BufferEviction() {
            @Override
            public boolean tryEviction(EvictionContext context) {
                if (self.tryEviction(context)) {
                    after.tryEviction(context);
                    return true;
                }
                return false;
            }

            @Override
            public String toString() {
                return self + " then " + after;
            }
        };
    }
}
