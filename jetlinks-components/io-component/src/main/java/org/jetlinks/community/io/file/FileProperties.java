package org.jetlinks.community.io.file;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties("file.manager")
public class FileProperties {

    private String storageBasePath = "./data/files";

    private DataSize readBufferSize = DataSize.ofKilobytes(64);

    private String accessBaseUrl;

    /**
     * 临时文件保存有效期,0为一直有效
     */
    private Duration tempFilePeriod = Duration.ZERO;
}
