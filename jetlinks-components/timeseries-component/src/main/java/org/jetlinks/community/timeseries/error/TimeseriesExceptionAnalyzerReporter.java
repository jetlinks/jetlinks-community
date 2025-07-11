package org.jetlinks.community.timeseries.error;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.exception.analyzer.ExceptionAnalyzerReporter;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.Objects;

@Slf4j
public class TimeseriesExceptionAnalyzerReporter extends ExceptionAnalyzerReporter {


    public TimeseriesExceptionAnalyzerReporter() {
        init();
    }

    void init() {
        addReporter(
            err -> err instanceof NoSuchBeanDefinitionException &&
                Objects.equals(((NoSuchBeanDefinitionException) err).getBeanType(),
                               TimeSeriesManager.class),
            err ->
                log.warn(wrapLog("请正确配置时序模块. 见文档: https://hanta.yuque.com/px7kg1/dev/xyr9mw8peqwtlhxc"), err));
    }
}
