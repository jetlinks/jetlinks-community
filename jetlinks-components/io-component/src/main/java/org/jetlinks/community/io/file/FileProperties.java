package org.jetlinks.community.io.file;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
@ConfigurationProperties("file.manager")
public class FileProperties {

    private String storageBasePath = "./data/files";

    private DataSize readBufferSize = DataSize.ofKilobytes(64);

}
