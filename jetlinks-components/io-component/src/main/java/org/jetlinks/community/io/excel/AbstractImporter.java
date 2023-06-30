package org.jetlinks.community.io.excel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.io.file.FileInfo;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.io.file.FileOption;
import org.jetlinks.community.io.utils.FileUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 抽象数据导入服务,导入数据并返回实时数据结果,导入结束后返回导入结果文件.
 *
 * @param <T> 数据类型
 * @author zhouhao
 * @see FileManager
 * @since 2.1
 */
@AllArgsConstructor
public abstract class AbstractImporter<T> {

    private final FileManager fileManager;

    private final WebClient client;

    protected abstract Mono<Void> handleData(Flux<T> data);

    protected abstract T newInstance();

    protected void customImport(ImportHelper<T> helper) {

    }

    public Flux<ImportResult<T>> doImport(String fileUrl) {
        String format = FileUtils.getExtension(fileUrl);

        ImportHelper<T> importHelper = new ImportHelper<>(this::newInstance, this::handleData);

        customImport(importHelper);

        return this
            .getInputStream(fileUrl)
            .flatMapMany(stream -> importHelper
                .doImport(stream, format, ImportResult::of,
                    buf -> fileManager
                        .saveFile(getResultFileName(fileUrl, format), buf, FileOption.tempFile)
                        .map(ImportResult::<T>of)));
    }

    protected String getResultFileName(String sourceFileUrl, String format) {

        return "导入结果_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")) + "." + format;
    }

    public enum ImportResultType {
        //数据
        data,
        //详情文件
        detailFile
    }

    @Getter
    @Setter
    public static class ImportResult<T> {
        @Schema(description = "导入结果类型")
        private ImportResultType type;

        @Schema(description = "行号,从数据的第一行为0开始")
        private long row;

        @Schema(description = "数据")
        private T data;

        @Schema(description = "是否成功")
        private boolean success;

        @Schema(description = "错误消息")
        private String message;

        @Schema(description = "导入结果详情文件地址")
        private String detailFile;

        public static <T> ImportResult<T> of(ImportHelper.Importing<T> importing) {
            ImportResult<T> result = new ImportResult<>();
            result.type = ImportResultType.data;
            result.row = importing.getRow();
            result.success = importing.isSuccess();
            result.message = importing.getErrorMessage();
            result.data = importing.getTarget();
            return result;
        }

        public static <T> ImportResult<T> of(FileInfo fileInfo) {
            ImportResult<T> result = new ImportResult<>();
            result.type = ImportResultType.detailFile;
            result.detailFile = fileInfo.getAccessUrl();
            return result;
        }

    }

    @SuppressWarnings("all")
    protected Mono<InputStream> getInputStream(String fileUrl) {
        return FileUtils.readInputStream(client, fileUrl);
    }
}
