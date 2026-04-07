/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
            err -> err.toString().contains("create_hypertable") &&
                err.toString().contains("does not exist"),
            err ->
                log.warn(wrapLog("请使用TimescaleDB.或者Postgres安装并启用TimescaleDB插件."), err));

        addReporter(
            err -> err instanceof NoSuchBeanDefinitionException &&
                Objects.equals(((NoSuchBeanDefinitionException) err).getBeanType(),
                               TimeSeriesManager.class),
            err ->
                log.warn(wrapLog("请正确配置时序模块. 见文档: https://hanta.yuque.com/px7kg1/dev/xyr9mw8peqwtlhxc"), err));
    }
}
