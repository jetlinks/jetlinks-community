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
package org.jetlinks.community.io.utils;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;


/**
 * @author liusq
 * @date 2024/8/20
 */
@Slf4j
public class ThumbnailUtils {
    private static final DefaultDataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();

    // 支持生成缩略图的文件格式
    public static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "tiff");

    public static BiFunction<Flux<DataBuffer>, Supplier<Boolean>, Flux<DataBuffer>> generateThumbnailFunction(String thumb) {
        return (flux, isThumbnail) -> flux
            .switchOnFirst((signal, f) -> {
                if (!signal.hasValue() || !isThumbnail.get()) {
                    return f;
                }
                if (thumb != null && thumb.contains("_")) {
                    String[] parts = thumb.split("_");
                    if (parts.length == 2) {
                        int width = CastUtils.castNumber(parts[0], s -> 0).intValue();
                        int height = CastUtils.castNumber(parts[1], s -> 0).intValue();
                        return ThumbnailUtils.generateThumbnail(flux, width, height);
                    }
                }
                double scale = CastUtils.castNumber(thumb, s -> 100).doubleValue() * 0.01;
                if (scale > 0 && scale < 1.0) {
                    return ThumbnailUtils.generateThumbnail(flux, scale);
                }
                return Flux.error(() -> new BusinessException("error.unsupported_scaling_parameters"));
            });
    }

    /**
     * 按照新比例生成对应图片的缩略图
     *
     * @param dataBufferFlux 需要被压缩的图片文件流
     * @param width          压缩后的宽度
     * @param height         压缩后的高度
     */
    public static Flux<DataBuffer> generateThumbnail(Flux<DataBuffer> dataBufferFlux, int width, int height) {
        return generateThumbnailInternal(dataBufferFlux, (inputStream, outputStream) ->
            Mono.fromCallable(() -> {
                try {
                    Thumbnails.of(inputStream).size(width, height).toOutputStream(outputStream);
                } finally {
                    closeStream(outputStream, inputStream);
                }
                return null;
            })
        );
    }

    /**
     * 按照新比例生成对应图片的缩略图
     *
     * @param dataBufferFlux 需要被压缩的图片文件流
     * @param scale          源文件的质量比例 范围0-1
     */

    public static Flux<DataBuffer> generateThumbnail(Flux<DataBuffer> dataBufferFlux, double scale) {
        return generateThumbnailInternal(dataBufferFlux, (inputStream, outputStream) ->
            Mono.fromCallable(() -> {
                try {
                    Thumbnails.of(inputStream).scale(scale).toOutputStream(outputStream);
                } finally {
                    closeStream(outputStream, inputStream);
                }
                return null;
            })
        );
    }

    private static Flux<DataBuffer> generateThumbnailInternal(
        Flux<DataBuffer> dataBufferFlux,
        BiFunction<InputStream, PipedOutputStream, Mono<Void>> thumbnailProcessor) {

        return DataBufferUtils
            .join(dataBufferFlux)
            .map(buffer -> buffer.asInputStream(true))
            .doOnDiscard(InputStream.class, ThumbnailUtils::closeStream)
            .flatMapMany(inputStream -> {
                PipedInputStream thumbnailInputStream = new PipedInputStream();
                PipedOutputStream thumbnailOutputStream = initPipedOutputStream(thumbnailInputStream);
                // 返回处理后的流
                return Flux.merge(thumbnailProcessor
                                      .apply(inputStream, thumbnailOutputStream)
                                      .subscribeOn(Schedulers.boundedElastic()),
                                  DataBufferUtils.readInputStream(
                                                     () -> thumbnailInputStream,
                                                     dataBufferFactory,
                                                     4096)
                                                 .subscribeOn(Schedulers.boundedElastic())
                           )
                           .cast(DataBuffer.class);
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    private static PipedOutputStream initPipedOutputStream(PipedInputStream inputStream) {
        PipedOutputStream thumbnailOutputStream;
        try {
            thumbnailOutputStream = new PipedOutputStream(inputStream);
        } catch (IOException e) {
            closeStream(inputStream);
            throw new RuntimeException(e);
        }
        return thumbnailOutputStream;
    }

    private static void closeStream(Closeable... streams) {
        for (Closeable stream : streams) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("Failed to close stream", e);
                }
            }
        }
    }
}
