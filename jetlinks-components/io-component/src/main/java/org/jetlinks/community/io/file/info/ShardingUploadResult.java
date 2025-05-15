package org.jetlinks.community.io.file.info;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 分片上传的返回
 *
 * @author gyl
 * @since 2.3
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShardingUploadResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean complete;

    private String fileId;

    private UploadResponse response;
}
