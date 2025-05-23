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
package org.jetlinks.community.configure.ui;

import lombok.SneakyThrows;
import org.hswebframework.web.exception.NotFoundException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.resource.ResourceResolver;
import org.springframework.web.reactive.resource.ResourceResolverChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@AutoConfiguration
public class JetLinksUiConfiguration {

    @AutoConfiguration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    static class WebFluxUiConfiguration {

        @Bean
        public WebFluxConfigurer emptyFileResolver() {
            return new WebFluxConfigurer() {
                @Override
                public void addResourceHandlers(@Nonnull ResourceHandlerRegistry registry) {
                    // 为 /ui/** 路径添加资源处理器，使用 EmptyFileResourceResolver 处理器
                    registry
                        .addResourceHandler("/ui/**")
                        .addResourceLocations("classpath:/ui/")
                        .resourceChain(false)
                        .addResolver(new EmptyFileResourceResolver());

                    // 为 /assets/** 路径添加资源处理器，使用 EmptyFileResourceResolver 处理器
                    registry
                        .addResourceHandler("/assets/**")
                        .addResourceLocations("classpath:/static/assets/")
                        .resourceChain(false)
                        .addResolver(new EmptyFileResourceResolver());

                    WebFluxConfigurer.super.addResourceHandlers(registry);
                }
            };
        }

    }


    static class EmptyFileResourceResolver implements ResourceResolver {

        @Override
        @Nonnull
        public Mono<Resource> resolveResource(ServerWebExchange exchange,
                                              @Nonnull
                                              String requestPath,
                                              @Nonnull
                                              List<? extends Resource> locations,
                                              ResourceResolverChain chain) {
            return chain
                .resolveResource(exchange, requestPath, locations)
                .flatMap(resource -> {
                    try {
                        if (resource.contentLength() == 0) {
                            // 返回一个有效的空资源，确保不会触发 404
                            return createEmpty(resource);
                        }
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                    return Mono.just(resource);
                })
                .switchIfEmpty(Mono.defer(() -> Flux
                    .fromIterable(locations)
                    .concatMap(location -> {
                        try {
                            Resource relative = location.createRelative(requestPath);
                            return createEmpty(relative);
                        } catch (IOException e) {
                            return Mono.error(e);
                        }
                    })
                    .next()));
        }

        @Override
        @Nonnull
        public Mono<String> resolveUrlPath(@Nonnull String resourcePath,
                                           @Nonnull List<? extends Resource> locations,
                                           ResourceResolverChain chain) {
            return chain.resolveUrlPath(resourcePath, locations);
        }


        @SneakyThrows
        private Mono<Resource> createEmpty(Resource resource) {
            return Mono.fromCallable(() -> createEmptyResource(resource));
        }

    }

    @SneakyThrows
    static Resource createEmptyResource(Resource resource) {
        try {
            String path = resource.getURL().getPath();
            String fileName = resource.getFilename();
            if (path.endsWith(File.separator) || !Objects.requireNonNull(fileName).contains(".")) {
                Resource index = resource;
                if (!path.endsWith(File.separator)) {
                    //文件夹应该以路径分隔符结尾
                    index = resource.createRelative(fileName + File.separator);
                }
                index = index.createRelative("/index.html");
                if (index.isReadable()) {
                    //文件夹下有可读的index.html
                    return index;
                }
            }
            return new EmptyFileResource(resource);
        } catch (FileNotFoundException e) {
            throw new NotFoundException
                .NoStackTrace("error.resource_not_found", e)
                .withSource("file.read", resource);
        }
    }

}
