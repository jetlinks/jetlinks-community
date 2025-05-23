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
package org.jetlinks.community.timescaledb;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.buffer.BufferProperties;
import org.jetlinks.community.timescaledb.impl.DefaultTimescaleDBDataWriter;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "timescaledb")
@Getter
@Setter
public class TimescaleDBProperties {

    private boolean enabled = false;

    //是否共享spring容器中的连接
    //需要平台也使用timescaledb
    private boolean sharedSpring = false;

    //当sharedSpring未false时,使用此连接配置.
    private R2dbcProperties r2dbc = new R2dbcProperties();

    //数据库的schema
    private String schema = "public";

    /**
     * 写入缓冲区配置
     *
     * @see DefaultTimescaleDBDataWriter
     */
    private BufferProperties writeBuffer = new BufferProperties();

    public TimescaleDBProperties() {
        writeBuffer.setFilePath("./data/timescaledb-buffer");
        writeBuffer.setSize(1000);
        writeBuffer.setParallelism(4);
        r2dbc.getPool().setMaxSize(64);
    }


}
