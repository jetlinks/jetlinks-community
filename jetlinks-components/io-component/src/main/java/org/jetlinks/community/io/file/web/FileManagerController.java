package org.jetlinks.community.io.file.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.DeleteAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.jetlinks.community.io.file.FileInfo;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.io.file.FileOption;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/file")
@AllArgsConstructor
@Resource(id= "file-manager",name = "文件管理")
@Tag(name = "需身份认证的文件管理")
public class FileManagerController {

    private final FileManager fileManager;

    @PostMapping("/upload")
    @Authorize(merge = false)
    @Operation(summary = "上传文件")
    public Mono<FileInfo> upload(@RequestPart("file") Mono<FilePart> partMono) {
        return partMono.flatMap(fileManager::saveFile);
    }

    @GetMapping("/{fileId}")
    @Authorize(ignore = true)
    @Operation(summary = "获取文件")
    public Mono<Void> read(@PathVariable String fileId,
                           ServerWebExchange exchange) {
        if (fileId.contains(".")) {
            fileId = fileId.substring(0, fileId.indexOf("."));
        }
        return exchange
            .getResponse()
            .writeWith(fileManager
                           .read(fileId, ctx -> {
                               Mono<Void> before;
                               //不是公开访问则需要登陆或者使用accessKey
                               if (!ctx.info().hasOption(FileOption.publicAccess)) {
                                   String key = exchange.getRequest().getQueryParams().getFirst("accessKey");
                                   //请求参数没有accessKey则校验当前用户是否登陆
                                   if (!StringUtils.hasText(key)) {
                                       before = Authentication
                                           .currentReactive()
                                           .switchIfEmpty(Mono.error(AccessDenyException::new))
                                           .then();
                                   } else {
                                       //校验accessKey
                                       if (ctx.info().accessKey().map(key::equalsIgnoreCase).orElse(false)) {
                                           before = Mono.empty();
                                       } else {
                                           before = Mono.error(AccessDenyException::new);
                                       }
                                   }
                               } else {
                                   before = Mono.empty();
                               }

                               return before.then(
                                   Mono.fromRunnable(() -> {
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
                                       exchange.getResponse().getHeaders().add("file-md5", ctx.info().getMd5());
                                       exchange.getResponse().getHeaders().add("file-sha256", ctx.info().getSha256());

                                       //文件流时下载文件
                                       if (mediaType.includes(MediaType.APPLICATION_OCTET_STREAM)) {
                                           exchange.getResponse().getHeaders().setContentDisposition(
                                               ContentDisposition
                                                   .builder("attachment")
                                                   .filename(ctx.info().getName(), StandardCharsets.UTF_8)
                                                   .build()
                                           );
                                       }
                                   })
                               );


                           }));
    }

    @DeleteMapping("/{fileId}")
    @DeleteAction
    @Operation(summary = "删除文件")
    public Mono<Integer> delete(@PathVariable String fileId) {
        return fileManager
            .delete(fileId);
    }

}
