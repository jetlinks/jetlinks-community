package org.jetlinks.community.io.utils;

import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;

import java.net.URLDecoder;

public class FileUtils {

    @SneakyThrows
    public static String getExtension(String url) {
        url = URLDecoder.decode(url, "utf8");
        if (url.contains("?")) {
            url = url.substring(0,url.lastIndexOf("?"));
        }
        if (url.contains("#")) {
            url = url.substring(0,url.lastIndexOf("#"));
        }
        return FilenameUtils.getExtension(url);
    }
}
