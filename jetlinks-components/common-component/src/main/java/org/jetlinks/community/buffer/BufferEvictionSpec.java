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

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
public class BufferEvictionSpec {

    public static final BufferEviction DEFAULT = new BufferEvictionSpec().build();

    //最大队列数量,超过则淘汰最旧的数据
    private int maxSize = -1;

    //最大死信数量,超过则淘汰dead数据
    private int maxDeadSize = Integer.getInteger("jetlinks.buffer.dead.limit", 100_0000);

    //根据磁盘空间淘汰数据
    private DataSize diskFree = DataSize.parse(System.getProperty("jetlinks.buffer.disk.free.threshold", "4GB"));

    //磁盘最大使用率
    private float diskThreshold;

    //判断磁盘空间大小的目录
    private String diskPath = System.getProperty("jetlinks.buffer.disk.free.path", "./");

    public BufferEviction build() {

        BufferEviction
            eviction = null,
            size = BufferEviction.limit(maxSize, maxDeadSize),
            disk = null;

        if (diskThreshold > 0) {
            disk = BufferEviction.disk(diskPath, diskThreshold);
        } else if (diskFree != null) {
            disk = BufferEviction.disk(diskPath, diskFree);
        }

        if (disk != null) {
            eviction = disk;
        }

        if (eviction == null) {
            eviction = size;
        } else {
            eviction = eviction.then(size);
        }

        return eviction;
    }


}
