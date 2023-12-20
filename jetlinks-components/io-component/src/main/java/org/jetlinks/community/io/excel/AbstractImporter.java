package org.jetlinks.community.io.excel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.io.file.FileInfo;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.io.file.FileOption;
import org.jetlinks.community.io.utils.FileUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.jetlinks.community.io.excel.ImportHelper.FORMAT_JSON;

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

    protected final FileManager fileManager;

    protected final WebClient client;

    protected abstract Mono<Void> handleData(Flux<T> data);

    protected abstract T newInstance();

    protected void customImport(ImportHelper<T> helper) {

    }

    /**
     * 写出导入结果文件
     *
     * @param fileUrl 原始导入文件地址
     * @param format  原始导入文件格式
     * @param buffer  结果文件流
     * @return 导入结果信息
     */
    protected Mono<ImportResult<T>> writeDetailFile(String fileUrl, String format, Flux<DataBuffer> buffer) {
        return fileManager
            .saveFile(getResultFileName(fileUrl, format), buffer, FileOption.tempFile)
            .map(ImportResult::of);
    }

    public Flux<ImportResult<T>> doImport(String fileUrl, String format) {
        ImportHelper<T> importHelper = new ImportHelper<>(this::newInstance, this::handleData);

        customImport(importHelper);
        //导入JSON
        if (FORMAT_JSON.equalsIgnoreCase(format)) {
            return importHelper
                .doImportJson(
                    FileUtils.readDataBuffer(client, fileUrl),
                    ImportResult::of,
                    buf -> writeDetailFile(fileUrl, format, buf));
        }
        //导入EXCEL
        return this
            .getInputStream(fileUrl)
            .flatMapMany(stream -> importHelper
                .doImport(stream, format, ImportResult::of,
                          buf -> writeDetailFile(fileUrl, format, buf)));
    }

    public Flux<ImportResult<T>> doImport(String fileUrl) {
        return doImport(fileUrl, FileUtils.getExtension(fileUrl));
    }

    protected String getResultFileName(String sourceFileUrl, String format) {
        return "导入结果_" + LocalDateTime
            .now()
            .format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")) + "." + format;
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
