package org.jetlinks.community.io.file;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class FileInfo {

    private String id;

    private String name;

    private String extension;

    private long length;

    private String md5;

    private String sha256;

    private long createTime;

    private String creatorId;

    private FileOption[] options;

    public MediaType mediaType() {
        if (!StringUtils.hasText(extension)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "text":
            case "txt":
                return MediaType.TEXT_PLAIN;
            case "js":
                return MediaType.APPLICATION_JSON;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public FileInfo withFileName(String fileName) {
        name = fileName;
        extension = FilenameUtils.getExtension(fileName);
        return this;
    }

}
