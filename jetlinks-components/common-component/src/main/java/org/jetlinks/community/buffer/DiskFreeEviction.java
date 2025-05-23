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


import org.jetlinks.community.utils.FormatUtils;
import org.springframework.util.unit.DataSize;

import java.io.File;
import java.util.Map;

class DiskFreeEviction extends AbstractBufferEviction {

    private final File path;
    private final long minUsableBytes;

    public DiskFreeEviction(File path, long minUsableBytes) {
        this.path = path;
        this.minUsableBytes = minUsableBytes;
    }

    private volatile long usableSpace = -1;
    private volatile long lastUpdateTime;

    @Override
    public boolean doEviction(EvictionContext context) {
        tryUpdate();

        if (freeOutOfThreshold()) {
            context.removeOldest(EvictionContext.BufferType.buffer);
            return true;
        }
        return false;
    }

    protected boolean freeOutOfThreshold() {
        return usableSpace != -1 && usableSpace <= minUsableBytes;
    }

    private void tryUpdate() {
        long now = System.currentTimeMillis();
        //1秒更新一次
        if (now - lastUpdateTime <= 1000) {
            return;
        }
        usableSpace = path.getUsableSpace();
        lastUpdateTime = now;
    }

    @Override
    protected void applyEventData(Map<String, Object> data) {
        data.put("usableSpace", DataSize.ofBytes(usableSpace).toMegabytes());
        data.put("minUsableBytes", DataSize.ofBytes(minUsableBytes).toMegabytes());
    }

    @Override
    public String toString() {
        return "DiskFree(path=" + path
            + ",space=" + FormatUtils.formatDataSize(usableSpace) + "/" + FormatUtils.formatDataSize(minUsableBytes) + ")";
    }
}
