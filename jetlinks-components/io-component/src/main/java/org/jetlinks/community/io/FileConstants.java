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
