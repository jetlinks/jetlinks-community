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
package org.jetlinks.community.io.file;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.jetlinks.community.io.FileConstants;
import org.jetlinks.community.io.utils.FileUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Getter
@Setter
public class FileInfo implements Serializable {
    private static final long serialVersionUID = 1L;

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

    private String accessUrl;
    //相对路径
    private String path;
    //所在服务id
    private String serverNodeId;

    public void withBasePath(String apiBashPath) {
        if (!apiBashPath.endsWith("/")) {
            apiBashPath = apiBashPath + "/";
        }
        accessUrl = apiBashPath + "file/" + id + "." + extension + "?accessKey=" + accessKey().orElse("");
    }

    public MediaType mediaType() {
        return FileUtils.getMediaTypeByExtension(extension);
    }


    public boolean hasOption(FileOption option) {
        if (options == null) {
            return false;
        }
        for (FileOption fileOption : options) {
            if (fileOption == option) {
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

    public String rename(Function<String, String> mapper) {
        if (name.contains(".")) {
            return mapper.apply(name.substring(0, name.lastIndexOf("."))) + "." + extension;
        }
        return mapper.apply(name) + "." + extension;
    }

    public Optional<String> accessKey() {
        return Optional
            .ofNullable(others)
            .map(map -> map.get(OTHER_ACCESS_KEY))
            .map(String::valueOf)
            .filter(StringUtils::hasText);
    }

    public Optional<Long> expirationTime() {
        return Optional
            .ofNullable(others)
            .map(map -> map.get(FileConstants.ContextKey.expiration.getKey()))
            .map(value -> CastUtils.castNumber(value).longValue());
    }

    @Override
    public String toString() {
        return id+"@"+serverNodeId;
    }
}
