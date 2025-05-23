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
package org.jetlinks.community.io;

import org.jetlinks.core.config.ConfigKey;

/**
 * @author gyl
 * @since 2.3
 */
public interface FileConstants {


    interface ContextKey {
        ConfigKey<Long> expiration = ConfigKey.of("fileExpirationTime", "过期时间(毫秒时间戳)", Long.class);

        /**
         * @see org.jetlinks.community.io.file.service.FileServiceProvider#getType()
         */
        ConfigKey<String> fileService = ConfigKey.of("fileService", "文件服务类型", String.class);

    }



}
