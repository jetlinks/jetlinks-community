package org.jetlinks.community.standalone.configuration;

import lombok.SneakyThrows;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.*;

@Configuration
public class ExecutorConfiguration {

    @Bean
    public AsyncConfigurer asyncConfigurer() {
        AsyncUncaughtExceptionHandler handler = new SimpleAsyncUncaughtExceptionHandler();

        return new AsyncConfigurer() {
            @Override
            @SneakyThrows
            public Executor getAsyncExecutor() {
                return threadPoolTaskExecutor().getObject();
            }

            @Override
            public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
                return handler;
            }
        };
    }

    @Bean
    public Scheduler reactorScheduler(ScheduledExecutorService executorService) {
        return Schedulers.fromExecutorService(executorService);
    }

    @Bean
    @ConfigurationProperties(prefix = "jetlinks.executor")
    @Primary
    public FactoryBean<ScheduledExecutorService> threadPoolTaskExecutor() {
        ThreadPoolExecutorFactoryBean executor = new ThreadPoolExecutorFactoryBean() {
            @Override
            protected ScheduledThreadPoolExecutor createExecutor(int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

                ScheduledThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
                poolExecutor.setMaximumPoolSize(maxPoolSize);
                poolExecutor.setRejectedExecutionHandler(rejectedExecutionHandler);
                poolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);

                return poolExecutor;
            }
        };
        executor.setThreadNamePrefix("jetlinks-thread-");
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);

        return (FactoryBean) executor;
    }
}
