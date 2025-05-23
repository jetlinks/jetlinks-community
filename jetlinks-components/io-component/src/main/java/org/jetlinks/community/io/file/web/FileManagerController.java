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
package org.jetlinks.community.io.file.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.jetlinks.community.io.file.*;
import org.jetlinks.community.io.utils.FileUtils;
import org.jetlinks.community.io.utils.ThumbnailUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.http.*;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.jetlinks.community.io.utils.ThumbnailUtils.SUPPORTED_EXTENSIONS;

@RestController
@RequestMapping("/file")
@AllArgsConstructor
@Tag(name = "文件管理")
public class FileManagerController {

    private final FileManager fileManager;

    private final FileEntityService fileEntityService;

    private final FileProperties properties;

    @PostMapping({"/upload", "/upload.{suffix:.*}"})
    @Authorize
    @Operation(summary = "上传文件")
    @SuppressWarnings("all")
    public Mono<FileInfo> upload(@RequestPart("file") Mono<FilePart> partMono,
                                 ServerWebExchange exchange,
                                 @RequestParam(required = false) String options) {
        String path = exchange.getRequest().getPath().value();
        String suffix = FileUtils.getExtension(path);
        return validatePermission(suffix, partMono)
            .flatMap(part -> fileManager.saveFile(part, FileOption.parse(options)));
    }


    //校验文件上传权限
    private Mono<FilePart> validatePermission(String suffix, Mono<FilePart> part) {
        return Mono
            .zip(
                Authentication
                    .currentReactive()
                    .switchIfEmpty(Mono.error(AccessDenyException.NoStackTrace::new)),
                part,
                (auth, _part) -> {
                    //校验权限
                    properties.validateUploadPermission(suffix, auth);
                    //校验是否允许上传
                    //properties.validateUploadPermission(suffix, _part);
                    return _part;
                }
            );
    }

    @PostMapping("/batch/upload")
    @Authorize
    @Operation(summary = "上传多个文件")
    public Flux<FileInfo> uploadFiles(@RequestPart("file") Flux<FilePart> partFlux,
                                      @RequestParam(required = false) String options) {
        return partFlux.flatMap(part -> fileManager.saveFile(part, FileOption.parse(options)));
    }

    @PutMapping("/update/{id}")
    @Resource(id = "file-manager", name = "文件管理")
    @SaveAction
    @Operation(summary = "根据ID修改文件属性")
    public Mono<Integer> updateFileInfo(@PathVariable @Parameter(description = "文件ID") String id,
                                        @RequestBody @Parameter(description = "文件实体") Mono<FileEntity> fileMono) {
        return fileEntityService.updateById(id, fileMono);
    }

    @PostMapping({"/upload/sharding/{sessionId}"})
    @Authorize
    @Operation(summary = "分片上传文件")
    @SuppressWarnings("all")
    public Mono<FileInfo> upload(@PathVariable String sessionId,
                                 @RequestParam int offset,
                                 @RequestParam int length,
                                 @RequestPart("file") Mono<FilePart> partMono,
                                 @RequestParam(required = false) String options) {
        String path = sessionId;
        String suffix = "";
        if (sessionId.contains(".")) {
            sessionId = sessionId.substring(0, sessionId.indexOf("."));
            suffix = FileUtils.getExtension(path);
        }
        String fSessionId = sessionId;
        return validatePermission(suffix, partMono)
            .flatMap(part -> fileManager
                .saveFile(
                    fSessionId,
                    part.filename(),
                    length,
                    offset,
                    part.content(),
                    FileOption.parse(options)));
    }


    @GetMapping("/{fileId}")
    @Authorize(ignore = true)
    @Operation(summary = "获取文件")
    public Mono<Void> read(@PathVariable String fileId,
                           @RequestParam(defaultValue = "false") @Parameter(description = "是否下载附件") boolean attachment,
                           @Parameter(description = "缩放参数(0-100)，thumb = 50,按原比例缩放50，thumb = 800_600，缩放为800*600尺寸") String thumb,
                           ServerWebExchange exchange) {
        return readAndWrite(fileManager, fileId, exchange);
    }

    private static Mono<Void> validatePermission(FileManager.ReaderContext ctx,
                                                 Authentication auth,
                                                 String key) {
        //不是公开访问则需要登陆或者使用accessKey
        if (!ctx.info().hasOption(FileOption.publicAccess)) {
            //请求参数没有accessKey则校验当前用户是否登陆
            if (!StringUtils.hasText(key)) {
                return auth == null ? Mono.error(new AccessDenyException()) : Mono.empty();
            } else {
                //校验accessKey
                if (ctx.info().accessKey().map(key::equalsIgnoreCase).orElse(false)) {
                    return Mono.empty();
                } else {
                    return Mono.error(AccessDenyException::new);
                }
            }
        } else {
            return Mono.empty();
        }
    }

    public static Mono<Void> readAndWrite(FileManager fileManager,
                                          String fileId,
                                          ServerWebExchange exchange) {
        if (fileId.contains(".")) {
            fileId = fileId.substring(0, fileId.indexOf("."));
        }
        boolean attachment = CastUtils.castBoolean(
            exchange.getRequest().getQueryParams().getFirst("attachment")
        );
        String thumb = exchange.getRequest().getQueryParams().getFirst("thumb");

        AtomicBoolean thumbnail = new AtomicBoolean(false);
        return exchange
            .getResponse()
            .writeWith(
                fileManager
                    .read(fileId, ctx -> {
                        String key = exchange.getRequest().getQueryParams().getFirst("accessKey");
                        if (SUPPORTED_EXTENSIONS.contains(ctx.info().getExtension()) && StringUtils.hasText(thumb)) {
                            thumbnail.set(true);
                        }
                        Mono<Void> before =
                            Authentication
                                .currentReactive()
                                .map(auth -> validatePermission(ctx, auth, key))
                                .defaultIfEmpty(Mono.defer(() -> validatePermission(ctx, null, key)))
                                .flatMap(Function.identity());

                        return before.then(
                            Mono.fromRunnable(() -> {
                                List<HttpRange> ranges = exchange
                                    .getRequest()
                                    .getHeaders()
                                    .getRange();
                                long position = 0;
                                long readTo = -1;
                                long fileLength = ctx.info().getLength();
                                boolean rangeRequest = CollectionUtils.isNotEmpty(ranges);
                                if (rangeRequest) {
                                    position = ranges.get(0).getRangeStart(fileLength);
                                    readTo = ranges.get(0).getRangeEnd(fileLength);
                                }
                                ctx.position(position);

                                HttpHeaders responseHeaders = exchange.getResponse().getHeaders();

                                MediaType mediaType = ctx.info().mediaType();
                                responseHeaders.setContentType(mediaType);
                                //支持range
                                responseHeaders.add(HttpHeaders.ACCEPT_RANGES, "bytes");
                                if (!thumbnail.get()) {
                                    if (readTo > 0) {

                                        responseHeaders.add(
                                            HttpHeaders.CONTENT_RANGE,
                                            "bytes " + position + "-" + readTo + "/" + fileLength);
                                        //range 错误
                                        if (readTo > fileLength) {
                                            exchange
                                                .getResponse()
                                                .setStatusCode(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
                                            ctx.length(0);
                                            return;
                                        }

                                        exchange.getResponse().setStatusCode(HttpStatus.PARTIAL_CONTENT);

                                        long readLength = readTo - position + 1;
                                        ctx.length(readLength);
                                        responseHeaders.setContentLength(readLength);
                                    } else {
                                        long contentLength = fileLength - position;
                                        exchange.getResponse().setStatusCode(HttpStatus.OK);
                                        responseHeaders.setContentLength(contentLength);
                                    }
                                }
                                responseHeaders.add("Content-MD5", ctx.info().getMd5());
                                responseHeaders.add("Digest", "sha-256=" + ctx.info().getSha256());

                                //设置下载或文件流时下载文件
                                if (attachment || mediaType.includes(MediaType.APPLICATION_OCTET_STREAM)) {
                                    responseHeaders.setContentDisposition(
                                        ContentDisposition
                                            .attachment()
                                            .filename(ctx.info().getName(), StandardCharsets.UTF_8)
                                            .build()
                                    );
                                }
                            })
                        );
                    })
                    .as(flux -> ThumbnailUtils.generateThumbnailFunction(thumb).apply(flux, thumbnail::get))

            );
    }

}
