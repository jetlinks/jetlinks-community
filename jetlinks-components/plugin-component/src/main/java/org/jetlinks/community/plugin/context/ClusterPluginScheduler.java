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
package org.jetlinks.community.plugin.context;

import io.opentelemetry.api.common.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.monitor.Monitor;
import org.jetlinks.plugin.core.PluginScheduler;
import org.jetlinks.community.TimerSpec;
import org.jetlinks.community.utils.ReactorUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支持集群的插件调度器
 *
 * @author zhouhao
 * @since 2.0
 */
@Slf4j
public class ClusterPluginScheduler implements PluginScheduler, Disposable {
    private final Map<String, Disposable> jobs = new ConcurrentHashMap<>();
    static final AttributeKey<String> JOB_NAME = AttributeKey.stringKey("jobName");

    private final String pluginId;

    private final Monitor monitor;

    public ClusterPluginScheduler(String pluginId) {
        this(pluginId, Monitor.noop());
    }

    public ClusterPluginScheduler(String pluginId, Monitor monitor) {
        this.pluginId = pluginId;
        this.monitor = monitor;
    }

    private String createJobName(String name) {
        return "plugin:" + pluginId + ":" + name;
    }

    @Override
    public Disposable interval(String name, Mono<Void> job, String cronExpression, boolean singleton) {
        Mono<Void> wrap = wrapJob(name, job);

        // 🌟企业版支持集群调度.
        Flux<Long> timer = TimerSpec.cron(cronExpression).flux();

        Disposable timerJob = timer
            .onBackpressureDrop(num -> monitor
                .logger()
                .warn("execute cron [{}] job [{}] dropped", cronExpression, name))
            .concatMap(ignore -> {
                monitor
                    .logger()
                    .debug("execute cron [{}] job [{}]", cronExpression, name);
                return wrap;
            })
            .subscribe();

        ReactorUtils.dispose(jobs.put(name, timerJob));

        return () -> {

            monitor
                .logger()
                .debug("stop cron [{}] job [{}]", cronExpression, name);

            jobs.remove(name, timerJob);
            timerJob.dispose();
        };
    }

    private Mono<Void> wrapJob(String name, Mono<Void> job) {
        return job
            .as(monitor
                    .tracer()
                    .traceMono("/interval",
                               (contextView, spanBuilder) -> spanBuilder.setAttribute(JOB_NAME, name)))
            .onErrorResume(err -> {
                monitor
                    .logger()
                    .warn("execute job [{}] error", name, err);
                return Mono.empty();
            });
    }

    @Override
    public Disposable interval(String name, Mono<Void> job, Duration interval, boolean singleton) {
        Mono<Void> wrap = wrapJob(name, job);
        
        Flux<Long> timer = Flux.interval(interval);

        Disposable timerJob = timer
            .onBackpressureDrop(num -> {
                monitor
                    .logger()
                    .warn("interval [{}] job [{}] dropped!", interval, name);
            })
            .concatMap(ignore -> {
                monitor
                    .logger()
                    .debug("execute interval [{}] job [{}]", interval, name);
                return wrap;
            })
            .subscribe();

        jobs.put(name, timerJob);

        return () -> {
            jobs.remove(name, timerJob);
            timerJob.dispose();
        };
    }

    @Override
    public void cancel(String name) {
        ReactorUtils.dispose(jobs.remove(name));
    }

    @Override
    public Disposable delay(Mono<Void> mono, Duration duration) {
        return Mono
            .delay(duration)
            .then(mono)
            .subscribe();
    }

    @Override
    public void dispose() {
        jobs.values().forEach(Disposable::dispose);
    }
}
