/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.io.utils;

import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.io.FilenameUtils;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.core.message.codec.http.HttpUtils;
import org.jetlinks.community.io.file.FileManager;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Set;

/**
 * 文件读取与媒体类型工具。
 *
 * 通用 URL 读取保留既有远程和本地文件能力；来自业务请求的托管文件必须通过
 * {@link #readManagedInputStream(FileManager, String)} 读取，避免绕过 {@link FileManager}
 * 直接访问网络或本地文件。
 */
public class FileUtils {

    /**
     * 可脚本化文档/脚本扩展名（不含 svg）。读响应 Content-Type 必须降为 octet-stream。
     */
    private static final Set<String> SCRIPTABLE_DOCUMENT_EXTENSIONS = Set.of(
        "html", "htm", "xhtml", "shtml",
        "js", "mjs", "cjs",
        "xml", "xsl", "xslt"
    );

    private static final Set<String> SVG_EXTENSIONS = Set.of("svg", "svgz");

    private static final MediaType IMAGE_SVG_XML = MediaType.parseMediaType("image/svg+xml");

    public static String getExtension(String url) {
        if (UrlCodecUtils.hasEncode(url)) {
            url = HttpUtils.urlDecode(url);
        }
        if (url.contains("?")) {
            url = url.substring(0, url.lastIndexOf("?"));
        }
        if (url.contains("#")) {
            url = url.substring(0, url.lastIndexOf("#"));
        }
        return FilenameUtils.getExtension(url);
    }

    public static String getFileName(String url) {
        url = HttpUtils.urlDecode(url);
        if (url.contains("?")) {
            url = url.substring(0, url.lastIndexOf("?"));
        }
        if (url.contains("#")) {
            url = url.substring(0, url.lastIndexOf("#"));
        }
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public static MediaType getMediaTypeByName(String name) {
        return getMediaTypeByExtension(FilenameUtils.getExtension(name));
    }


    /**
     * 根据文件拓展名获取MediaType
     *
     * @param extension extension
     * @return MediaType
     */
    public static MediaType getMediaTypeByExtension(String extension) {
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
            case "svg":
                return MediaType.parseMediaType("image/svg+xml");
            case "tiff":
            case "tif":
                return MediaType.parseMediaType("image/tiff");
            case "webp":
                return MediaType.parseMediaType("image/webp");
            case "mp4":
                return MediaType.parseMediaType("video/mp4");
            case "flv":
                return MediaType.parseMediaType("video/x-flv");
            case "text":
            case "txt":
                return MediaType.TEXT_PLAIN;
            case "html":
                return MediaType.TEXT_HTML;
            case "md":
                return MediaType.TEXT_MARKDOWN;
            case "css":
                return MediaType.parseMediaType("text/css");
            case "js":
                return MediaType.parseMediaType("text/javascript");
            case "xml":
                return MediaType.TEXT_XML;
            case "json":
                return MediaType.APPLICATION_JSON;
            case "pdf":
                return MediaType.APPLICATION_PDF;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 判断扩展名是否可在浏览器中作为文档或脚本执行。
     *
     * <p>用于读侧强制附件下载与 CSP sandbox，避免用户文件在本源执行（存储型 XSS / CWE-79）。
     * 空白或空扩展名视为不可执行。</p>
     *
     * @param extension 文件扩展名，可不带点
     * @return 是否为可执行内容
     */
    public static boolean isActiveContentExtension(String extension) {
        String normalized = normalizeExtension(extension);
        return SCRIPTABLE_DOCUMENT_EXTENSIONS.contains(normalized) || SVG_EXTENSIONS.contains(normalized);
    }

    /**
     * 判断扩展名是否为可脚本化文档（不含 svg/svgz）。
     *
     * <p>这类文件必须以降级 MIME {@code application/octet-stream} 返回，避免浏览器当页面执行。
     * svg 仍保留 {@code image/svg+xml}，以便 {@code <img src>} 显示。</p>
     *
     * @param extension 文件扩展名，可不带点
     * @return 是否为可脚本化文档
     */
    public static boolean isScriptableDocumentExtension(String extension) {
        return SCRIPTABLE_DOCUMENT_EXTENSIONS.contains(normalizeExtension(extension));
    }

    /**
     * 将原始探测类型转换为读接口安全响应类型。
     *
     * <p>不要改 {@link #getMediaTypeByExtension(String)} 的全局映射：文档生成等内部调用仍需要
     * html → {@code text/html}。仅文件读响应走本方法。</p>
     *
     * @param extension 文件扩展名
     * @param original  原始 MediaType
     * @return 安全响应 MediaType
     */
    public static MediaType safeResponseMediaType(String extension, MediaType original) {
        if (isScriptableDocumentExtension(extension)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        if (SVG_EXTENSIONS.contains(normalizeExtension(extension))) {
            // 一律 image/svg+xml：即使原始探测类型异常，也禁止把 svg 当文档执行；<img> 仍可显示。
            return IMAGE_SVG_XML;
        }
        return original == null ? MediaType.APPLICATION_OCTET_STREAM : original;
    }

    /**
     * 是否强制以附件方式下载。
     *
     * <p>可执行扩展名一律 attachment，阻断顶级导航执行；既有 {@code attachment=true}
     * 与 {@code application/octet-stream} 下载行为保持不变。</p>
     *
     * @param extension         文件扩展名
     * @param attachmentRequest 请求是否显式要求下载
     * @param mediaType         即将写出的响应类型
     * @return 是否设置 Content-Disposition: attachment
     */
    public static boolean shouldForceAttachment(String extension,
                                                boolean attachmentRequest,
                                                MediaType mediaType) {
        if (attachmentRequest || isActiveContentExtension(extension)) {
            return true;
        }
        return mediaType != null && mediaType.includes(MediaType.APPLICATION_OCTET_STREAM);
    }

    private static String normalizeExtension(String extension) {
        if (!StringUtils.hasText(extension)) {
            return "";
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }

    public static Mono<InputStream> dataBufferToInputStream(Flux<DataBuffer> dataBufferFlux) {
        NettyDataBufferFactory factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

        return DataBufferUtils
            .join(dataBufferFlux
                      .map(buffer -> {
                          if (buffer instanceof NettyDataBuffer) {
                              return buffer;
                          }
                          try {
                              return factory.wrap(buffer.asByteBuffer());
                          } finally {
                              DataBufferUtils.release(buffer);
                          }
                      }))
            .map(buffer -> buffer.asInputStream(true));

    }

    /**
     * 读取平台托管文件。
     *
     * 仅接受 {@link FileManager} 文件 ID 或包含 {@code /file/{id}} 的平台文件访问地址。
     * 访问地址只用于解析文件 ID，不会发起 HTTP 请求或读取本地路径。
     *
     * @param fileManager 文件管理器
     * @param fileUrlOrId 平台文件访问地址或文件 ID
     * @return 文件输入流，调用方使用完毕后必须关闭
     */
    public static Mono<InputStream> readManagedInputStream(FileManager fileManager,
                                                           String fileUrlOrId) {
        return Mono.defer(() -> dataBufferToInputStream(
            fileManager.read(resolveManagedFileId(fileUrlOrId))
        ));
    }

    /**
     * 从平台文件访问地址或文件 ID 中解析托管文件 ID。
     *
     * @param fileUrlOrId 平台文件访问地址或文件 ID
     * @return 托管文件 ID
     * @throws ValidationException 输入不是托管文件 ID 或平台文件访问地址
     */
    public static String resolveManagedFileId(String fileUrlOrId) {
        if (!StringUtils.hasText(fileUrlOrId)) {
            throw unsupportedManagedFile();
        }

        URI uri;
        try {
            uri = URI.create(fileUrlOrId);
        } catch (IllegalArgumentException e) {
            throw unsupportedManagedFile();
        }

        String path = uri.getPath();
        if (!uri.isAbsolute()
            && !fileUrlOrId.contains("/")
            && !fileUrlOrId.contains("\\")) {
            if (uri.getQuery() == null
                && uri.getFragment() == null
                && StringUtils.hasText(path)) {
                return path;
            }
            throw unsupportedManagedFile();
        }

        int filePathIndex = path == null ? -1 : path.lastIndexOf("/file/");
        if (filePathIndex < 0) {
            throw unsupportedManagedFile();
        }

        String fileName = path.substring(filePathIndex + "/file/".length());
        if (!StringUtils.hasText(fileName)
            || fileName.contains("/")
            || fileName.contains("\\")) {
            throw unsupportedManagedFile();
        }

        int extensionIndex = fileName.indexOf('.');
        if (extensionIndex == 0) {
            throw unsupportedManagedFile();
        }
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    }

    private static ValidationException unsupportedManagedFile() {
        return new ValidationException.NoStackTrace("error.only_managed_file_supported");
    }

    public static Flux<DataBuffer> readDataBuffer(WebClient client,
                                                  String fileUrl) {
        if (fileUrl.startsWith("http")) {
            return client
                .get()
                .uri(fileUrl)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToFlux(DataBuffer.class);
        } else {
            return DataBufferUtils.readInputStream(
                () -> Files.newInputStream(Paths.get(fileUrl)),
                new NettyDataBufferFactory(ByteBufAllocator.DEFAULT),
                256 * 1024);
        }
    }

    public static Mono<InputStream> readInputStream(WebClient client,
                                                    String fileUrl) {
        return Mono.defer(() -> {
            if (fileUrl.startsWith("http")) {
                return client
                    .get()
                    .uri(fileUrl)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .exchangeToMono(clientResponse -> clientResponse.bodyToMono(Resource.class))
                    .flatMap(resource -> Mono.fromCallable(resource::getInputStream));
            } else {
                return Mono.fromCallable(() -> new FileInputStream(fileUrl));
            }
        });

    }


    /**
     * 计算文件流的校验和
     *
     * @param digest     检验算法
     * @param dataBuffer 文件流
     * @return 文件流
     */
    public static DataBuffer updateDigest(MessageDigest digest, DataBuffer dataBuffer) {
        dataBuffer = DataBufferUtils.retain(dataBuffer);

        try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
            iterator.forEachRemaining(digest::update);
        }

        DataBufferUtils.release(dataBuffer);
        return dataBuffer;
    }
}
