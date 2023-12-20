package org.jetlinks.community.io.excel;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBufAllocator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.CastUtil;
import org.hswebframework.reactor.excel.CellDataType;
import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.web.api.crud.entity.Entity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.validator.CreateGroup;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.core.metadata.Jsonable;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ImportHelper<T> {

    public static final String FORMAT_JSON = "json";

    public static final String FORMAT_XLSX = "xlsx";

    public static final String FORMAT_CSV = "csv";

    /**
     * 实体构造器
     */
    private final Supplier<T> instanceSupplier;

    /**
     * 数据处理器,应当支持事务和幂等.
     */
    private final Function<Flux<T>, Mono<Void>> handler;


    /**
     * 批量处理缓冲区大小
     */
    private int bufferSize = 200;

    /**
     * 当批量处理失败时,是否回退为单条数据处理.
     */
    private boolean fallbackSingle;

    /**
     * 自定义表头信息
     */
    private final List<ExcelHeader> customHeaders = new ArrayList<>();

    private Consumer<T> afterRead = t -> {
        if (t instanceof Entity) {
            ((Entity) t).tryValidate(CreateGroup.class);
        } else {
            ValidatorUtils.tryValidate(t, CreateGroup.class);
        }
    };

    public ImportHelper(Supplier<T> supplier, Function<Flux<T>, Mono<Void>> handler) {
        this.instanceSupplier = supplier;
        this.handler = handler;
    }

    public ImportHelper<T> addHeader(String key, String text) {
        return addHeader(new ExcelHeader(key, text, CellDataType.STRING));
    }

    public ImportHelper<T> addHeader(ExcelHeader header) {
        customHeaders.add(header);
        return this;
    }

    public ImportHelper<T> addHeaders(Collection<ExcelHeader> header) {
        customHeaders.addAll(header);
        return this;
    }

    public ImportHelper<T> fallbackSingle(boolean fallbackSingle) {
        this.fallbackSingle = fallbackSingle;
        return this;
    }

    public ImportHelper<T> bufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    public ImportHelper<T> afterReadValidate(Class<?>... group) {
        return afterRead(t -> {
            if (t instanceof Entity) {
                ((Entity) t).tryValidate(group);
            } else {
                ValidatorUtils.tryValidate(t, group);
            }
        });
    }

    public ImportHelper<T> afterRead(Consumer<T> afterRead) {
        this.afterRead = afterRead;
        return this;
    }

    private List<ExcelHeader> createHeaders() {
        List<ExcelHeader> headers = new ArrayList<>(
            ExcelUtils.getHeadersForRead(getInstanceType())
        );
        headers.addAll(customHeaders);
        return headers;
    }

    @SuppressWarnings("all")
    public <R> Flux<R> doImportJson(Flux<DataBuffer> inputStream,
                                    Function<Importing<T>, R> resultMapper,
                                    Function<Flux<DataBuffer>, Mono<R>> infoWriter) {

        Flux<Importing<T>> importingFlux = ObjectMappers
            .parseJsonStream(inputStream, Map.class)
            .map(map -> CastUtil.<Map<String,Object>>cast(map))
            .index(this::createImporting)
            .buffer(bufferSize)
            .concatMap(buffer -> this
                .executeImport(Collections2.filter(buffer, Importing::isSuccess))
                .thenMany(Flux.fromIterable(buffer)));

        return doImport0(importingFlux, FORMAT_XLSX, resultMapper, infoWriter);
    }

    private <R> Flux<R> doImport0(Flux<Importing<T>> importingStream,
                                  String errorFileFormat,
                                  Function<Importing<T>, R> resultMapper,
                                  Function<Flux<DataBuffer>, Mono<R>> infoWriter) {
        Flux<Importing<T>> cache = importingStream
            .replay()
            .refCount(1, Duration.ofMillis(100))
            .as(LocaleUtils::transform);

        List<ExcelHeader> headers = createHeaders();
        headers.add(
            new ExcelHeader(
                "$_result", LocaleUtils.resolveMessage("import.header.result", "导入结果"), CellDataType.STRING
            )
        );
        return Flux.merge(
            cache.mapNotNull(resultMapper),
            ExcelUtils
                .write(headers, cache
                    .map(importing -> {
                        Map<String, Object> map = new LinkedHashMap<>(importing.getSource());
                        if (importing.isSuccess()) {
                            map.put("$_result", LocaleUtils.resolveMessage(
                                "import.result.success", "成功"));
                        } else {
                            String errorMessage = importing.getErrorMessage();
                            map.put("$_result", LocaleUtils.resolveMessage(
                                "import.result.error", "失败:" + errorMessage, errorMessage));
                        }
                        return map;
                    }), errorFileFormat)
                .as(infoWriter)
        );
    }

    public <R> Flux<R> doImport(InputStream inputStream,
                                String format,
                                Function<Importing<T>, R> resultMapper,
                                Function<Flux<DataBuffer>, Mono<R>> infoWriter) {
        if (FORMAT_JSON.equals(format)) {
            return doImportJson(DataBufferUtils.readInputStream(
                                    () -> inputStream,
                                    new NettyDataBufferFactory(ByteBufAllocator.DEFAULT),
                                    256 * 1024),
                                resultMapper,
                                infoWriter);
        }
        return doImport0(readExcelFile(inputStream, format), format, resultMapper, infoWriter);
    }

    @SuppressWarnings("all")
    protected Class<T> getInstanceType() {
        return (Class<T>) instanceSupplier.get().getClass();
    }

    public Flux<Importing<T>> readExcelFile(InputStream inputStream, String format) {

        return ExcelUtils
            .<Map<String, Object>>read(LinkedHashMap::new,
                                       createHeaders(),
                                       inputStream,
                                       format)
            .map(data -> Maps.filterValues(data, obj -> !ObjectUtils.isEmpty(obj)))
            .index(this::createImporting)
            .buffer(bufferSize)
            .concatMap(buffer -> this
                .executeImport(Collections2.filter(buffer, Importing::isSuccess))
                .thenMany(Flux.fromIterable(buffer)));
    }

    private Mono<Void> executeImport(Collection<Importing<T>> buffer) {
        if (CollectionUtils.isEmpty(buffer)) {
            return Mono.empty();
        }

        Mono<Void> batchHandler = Flux
            .fromIterable(buffer)
            .map(Importing::getTarget)
            .as(handler);

        //错误发生时回退到单个处理
        if (fallbackSingle && buffer.size() > 1) {
            return batchHandler
                .onErrorResume(err -> Flux
                    .fromIterable(buffer)
                    .flatMap(importing -> handler
                        .apply(Flux.just(importing.target))
                        .onErrorResume(e -> {
                            importing.error(e);
                            return Mono.empty();
                        }))
                    .then());
        }
        return batchHandler
            .onErrorResume(err -> {
                for (Importing<T> importing : buffer) {
                    importing.batchError = true;
                    importing.error(err);
                }
                return Mono.empty();
            });
    }

    private Importing<T> createImporting(long index, Map<String, Object> data) {
        T instance = instanceSupplier.get();
        Importing<T> importing = new Importing<>(index, data, instance);
        try {
            if (instance instanceof Entity) {
                ((Entity) instance).copyFrom(data);
            } else if (instance instanceof Jsonable) {
                ((Jsonable) instance).fromJson(new JSONObject(data));
            } else {
                FastBeanCopier.copy(data, instance);
            }
            if (afterRead != null) {
                afterRead.accept(instance);
            }
        } catch (Throwable e) {
            importing.error(e);
        }
        return importing;
    }


    @RequiredArgsConstructor
    @Getter
    public static class Importing<T> {
        private final long row;
        private final Map<String, Object> source;
        private final T target;
        private boolean batchError;
        private boolean success = true;

        @Getter(AccessLevel.PRIVATE)
        private transient Throwable error;

        public String getErrorMessage() {
            //todo 更多异常信息判断
            if (error instanceof NumberFormatException) {
                String msg = error.getMessage();
                if (null == msg){
                    return LocaleUtils.resolveMessage("error.number_format_error_no_arg", "无法转换为数字");
                }
                if (msg.contains("\"")) {
                    String ch = msg.substring(msg.indexOf("\"") + 1, msg.lastIndexOf("\""));
                    return LocaleUtils.resolveMessage("error.number_format_error", "无法转换[" + ch + "]为数字", ch);
                }
            }
            return error == null ? null : error.getLocalizedMessage();
        }

        void error(Throwable error) {
            this.success = false;
            this.error = error;
        }
    }

}
