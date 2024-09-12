package org.jetlinks.community.io.utils;

import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.io.FilenameUtils;
import org.jetlinks.core.message.codec.http.HttpUtils;
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
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileUtils {

    public static String getExtension(String url) {
        if (UrlCodecUtils.hasEncode(url)){
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

}
