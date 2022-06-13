package org.jetlinks.community.io.file.web;

import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.jetlinks.community.io.file.FileInfo;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.io.file.FileProperties;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/file")
@AllArgsConstructor
public class FileManagerController {

    private final FileProperties properties;

    private final FileManager fileManager;

    @PostMapping("/upload")
    @Authorize(merge = false)
    @Operation(summary = "上传文件")
    public Mono<FileInfo> upload(@RequestPart("file") Mono<FilePart> partMono) {
        return partMono.flatMap(fileManager::saveFile);
    }

    @GetMapping("/{fileId}")
    @Authorize(merge = false)
    @Operation(summary = "获取文件")
    public Mono<Void> read(@PathVariable String fileId,
                           ServerWebExchange exchange) {

        return exchange
            .getResponse()
            .writeWith(fileManager
                           .read(fileId, ctx -> {
                               List<HttpRange> ranges = exchange
                                   .getRequest()
                                   .getHeaders()
                                   .getRange();
                               long position = 0;
                               if (ranges.size() != 0) {
                                   position = ranges.get(0).getRangeStart(ctx.info().getLength());
                               }
                               ctx.position(position);
                               MediaType mediaType = ctx.info().mediaType();
                               exchange.getResponse().getHeaders().setContentType(mediaType);
                               exchange.getResponse().getHeaders().setContentLength(ctx.info().getLength());
                               //文件流时下载文件
                               if (mediaType.includes(MediaType.APPLICATION_OCTET_STREAM)) {
                                   exchange.getResponse().getHeaders().setContentDisposition(
                                       ContentDisposition
                                           .builder("attachment")
                                           .filename(ctx.info().getName(), StandardCharsets.UTF_8)
                                           .build()
                                   );
                               }
                               return Mono.empty();
                           }));
    }

    //用于集群间获取文件
    @GetMapping("/{clusterNodeId}/{fileId}")
    @Authorize(ignore = true)
    @Operation(summary = "集群间获取文件", hidden = true)
    public Mono<Void> readFromCluster(@PathVariable String clusterNodeId,
                                      @PathVariable String fileId,
                                      ServerWebExchange exchange) {
        if (Objects.equals(clusterNodeId, properties.getServerNodeId())) {
            //读取自己
            return Mono.error(new IllegalArgumentException("error.file_read_loop"));
        }
        //校验key
        if (!Objects.equals(exchange.getRequest().getHeaders().getFirst(FileProperties.clusterKeyHeader),
                            properties.getClusterKey())) {
            return Mono.error(new AccessDenyException());
        }
        return read(fileId, exchange);
    }

    //用于集群间保存文件
    @PostMapping("/{clusterNodeId}")
    @Authorize(ignore = true)
    @Operation(summary = "集群间获取文件", hidden = true)
    public Mono<ResponseEntity<FileInfo>> saveFromCluster(@PathVariable String clusterNodeId,
                                                          @RequestPart("file") Mono<FilePart> partMono,
                                                          @RequestHeader(FileProperties.clusterKeyHeader) String key) {
        if (!Objects.equals(clusterNodeId, properties.getServerNodeId())) {
            return Mono.error(new IllegalArgumentException("error.file_read_loop"));
        }
        //校验key
        if (!Objects.equals(key, properties.getClusterKey())) {
            return Mono.error(new AccessDenyException());
        }
        return upload(partMono)
            .map(ResponseEntity::ok);
    }
}
