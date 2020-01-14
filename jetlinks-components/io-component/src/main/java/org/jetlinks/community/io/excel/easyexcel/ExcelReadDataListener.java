package org.jetlinks.community.io.excel.easyexcel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.io.excel.RowResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.InputStream;

/**
 * 不能用spring管理，每次调用都需要new
 *
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class ExcelReadDataListener<T> extends AnalysisEventListener<T> {


    private FluxSink<RowResult<T>> sink;

    public ExcelReadDataListener(FluxSink<RowResult<T>> sink) {
        this.sink = sink;
    }


    public static <T> Flux<RowResult<T>> of(InputStream fileInputStream, Class<T> clazz) {
        return Flux.create(sink -> {
            EasyExcel.read(fileInputStream, clazz, new ExcelReadDataListener<>(sink)).sheet().doRead();
        });
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) {
        sink.error(exception);
    }

    /**
     * 这个每一条数据解析都会来调用
     */
    @Override
    public void invoke(T data, AnalysisContext analysisContext) {
        RowResult<T> result=new RowResult<>();
        result.setResult(data);
        result.setRowIndex(analysisContext.readRowHolder().getRowIndex());

        sink.next(result);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        sink.complete();
    }

    @Override
    public boolean hasNext(AnalysisContext context) {
        return !sink.isCancelled();
    }
}
