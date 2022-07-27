package org.jetlinks.community.io.file;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
public class FileInfo {

    public static final String OTHER_ACCESS_KEY = "accessKey";

    private String id;

    private String name;

    private String extension;

    private long length;

    private String md5;

    private String sha256;

    private long createTime;

    private String creatorId;

    private FileOption[] options;

    private Map<String, Object> others;

    public MediaType mediaType() {
        if (!StringUtils.hasText(extension)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "gif":
                return MediaType.IMAGE_GIF;
            case "mp4":
                return MediaType.parseMediaType("video/mp4");
            case "flv":
                return MediaType.parseMediaType("video/x-flv");
            case "text":
            case "txt":
                return MediaType.TEXT_PLAIN;
            case "js":
                return MediaType.APPLICATION_JSON;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }


    public boolean hasOption(FileOption option){
        if(options==null){
            return false;
        }
        for (FileOption fileOption : options) {
            if(fileOption==option){
                return true;
            }
        }
        return false;
    }
    public FileInfo withFileName(String fileName) {
        name = fileName;
        extension = FilenameUtils.getExtension(fileName);
        return this;
    }

    public synchronized FileInfo withOther(String key, Object value) {
        if (others == null) {
            others = new HashMap<>();
        }
        others.put(key, value);
        return this;
    }

    public FileInfo withAccessKey(String accessKey) {
        withOther(OTHER_ACCESS_KEY, accessKey);
        return this;
    }

    public Optional<String> accessKey() {
        return Optional
            .ofNullable(others)
            .map(map -> map.get(OTHER_ACCESS_KEY))
            .map(String::valueOf)
            .filter(StringUtils::hasText);
    }


}
