package org.jetlinks.community.io.utils;

import io.netty.buffer.ByteBufAllocator;
import lombok.SneakyThrows;
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

public class FileUtils {

    public static String getExtension(String url) {
        url = HttpUtils.urlDecode(url);
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
