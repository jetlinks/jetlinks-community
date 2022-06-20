package org.jetlinks.community.configure.trace;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.jetlinks.community.configure.cluster.ClusterProperties;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.trace.EventBusSpanExporter;
import org.jetlinks.core.trace.TraceHolder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TraceProperties.class)
//@ConditionalOnProperty(prefix = "trace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceConfiguration {

    @Bean
    public TraceWebFilter traceWebFilter() {
        return new TraceWebFilter();
    }

    //推送跟踪信息到eventBus中
    @Bean
    public SpanProcessor eventBusSpanExporter(EventBus eventBus) {
        return SimpleSpanProcessor.create(
            EventBusSpanExporter.create(eventBus)
        );
    }

    @Bean
    public OpenTelemetry createTelemetry(ObjectProvider<SpanProcessor> spanProcessors,
                                         ClusterProperties clusterProperties,
                                         TraceProperties traceProperties) {
        SdkTracerProviderBuilder sdkTracerProvider = SdkTracerProvider.builder();
        spanProcessors.forEach(sdkTracerProvider::addSpanProcessor);
        traceProperties.buildProcessors().forEach(sdkTracerProvider::addSpanProcessor);
        SdkTracerProvider tracerProvider = sdkTracerProvider
            .setResource(Resource
                             .builder()
                             .put("service.name", clusterProperties.getId())
                             .build())
            .build();

        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));

        OpenTelemetrySdk telemetry= OpenTelemetrySdk
            .builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build();
        TraceHolder.setup(telemetry);
        try {
            GlobalOpenTelemetry.set(telemetry);
        }catch (Throwable ignore){

        }
        return telemetry;
    }

    @Bean
    public WebClientCustomizer traceWebClientCustomizer(OpenTelemetry openTelemetry) {
        return builder -> builder
            .filters(filters -> {
                if (!filters.contains(TraceExchangeFilterFunction.instance())) {
                    filters.add(TraceExchangeFilterFunction.instance());
                }
            });
    }
}
