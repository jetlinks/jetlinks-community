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

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.define.AuthorizeDefinitionContext;
import org.hswebframework.web.authorization.define.AuthorizeDefinitionCustomizer;
import org.hswebframework.web.authorization.define.ResourceDefinition;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.jetlinks.community.io.file.service.LocalFileServiceProvider;
import org.jetlinks.community.io.utils.FileUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@ConfigurationProperties("file.manager")
public class FileProperties implements AuthorizeDefinitionCustomizer {
    static final Map<String, SuffixPermission> DEFAULT_SUFFIX_MAPPING = new ConcurrentHashMap<>();

    public static final String PERMISSION_ID = "file-manager";

    /**
     * 指定默认文件存储服务，在上下文未指定时生效
     *
     * @see org.jetlinks.community.io.file.service.FileServiceProvider#getType()
     */
    private String defaultService = LocalFileServiceProvider.TYPE;

    private String storageBasePath = "./data/files";

    private DataSize readBufferSize = DataSize.ofKilobytes(64);

    private String accessBaseUrl;

    /**
     * 临时文件保存有效期,0为一直有效
     */
    private Duration tempFilePeriod = Duration.ZERO;

    private Duration shardingTimeout = Duration.ofMinutes(10);

    /**
     * <pre>{@code
     *
     *  permission:
     *    enabled: true
     *    default-suffix: [jar,zip,jpg,jpeg,png,gif,bmp,webp,xls,xlsx,csv]
     *    suffix:
     *       jar: [jar,zip]
     *       img: [jpg,jpeg,png,gif,bmp,webp]
     * }</pre>
     */
    private SuffixPermission permission = new SuffixPermission();


    public void validateUploadPermission(String suffix, Authentication auth) {
        if (!permission.isEnabled()) {
            return;
        }
        //允许上传任意文件
        boolean uploadAny = auth.hasPermission(PERMISSION_ID, "upload-any");
        if (uploadAny) {
            return;
        }
        if (!permission.getSuffixes().containsKey(suffix)
            || !auth.hasPermission(PERMISSION_ID, "upload-" + suffix)) {
            throw new AccessDenyException.NoStackTrace();
        }
    }

    public void validateUploadPermission(String suffix, FilePart part) {
        if (!permission.enabled) {
            return;
        }
        String name = FileUtils.getExtension(part.filename());
        Set<String> allowed;
        if (StringUtils.hasText(suffix)) {
            allowed = permission.getSuffixes().get(suffix);
        } else {
            allowed = permission.getDefaultSuffix();
        }
        if (allowed == null || !allowed.contains(name)) {
            throw new AccessDenyException.NoStackTrace("error.unsupported_file_types", name);
        }
    }

    @Override
    public void custom(AuthorizeDefinitionContext context) {
        if (permission.enabled) {
            ResourceDefinition definition = ResourceDefinition
                .of(PERMISSION_ID, "文件管理");

            definition.addAction("upload-any", "上传任意文件");

            for (String suffix : permission.getSuffixes().keySet()) {
                definition.addAction("upload-" + suffix, "上传" + suffix + "文件");
            }
            context.addResource(definition);
        }
    }

    @Getter
    @Setter
    public static class SuffixPermission {

        private boolean enabled = false;

        //不带后缀
        private Set<String> defaultSuffix = Sets
            .newHashSet("zip",
                        "xls", "xlsx", "csv",
                        "jpg", "jpeg", "png", "gif", "bmp", "webp");

        private Map<String, Set<String>> suffixes = new HashMap<>();
    }
}
