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
package org.jetlinks.community.io.file.repository;

import lombok.AllArgsConstructor;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.io.file.FileInfo;
import org.jetlinks.core.Lazy;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author gyl
 * @since 2.3
 */
@AllArgsConstructor
public class FileInfoRepositoryHelper {

    private final List<FileInfoRepository> repositoryList;

    public FileInfoRepositoryHelper(ObjectProvider<FileInfoRepository> repositories) {
        this.repositoryList = repositories
            .stream()
            .sorted(Comparator.comparingInt(Ordered::getOrder))
            .collect(Collectors.toList());
    }

    /**
     * 获取最高优先级的文件存储方式
     *
     * @return 文件存储方式
     */
    public FileInfoRepository getRepository() {
        return repositoryList.get(0);
    }

    /**
     * 按优先级查找支持指定文件id且获取文件信息成功的第一个文件存储方式及对应文件信息
     *
     * @param id 文件id
     * @return 文件存储方式
     */
    public Mono<Tuple2<FileInfoRepository, FileInfo>> findRepositorySingle(String id,
                                                                           Function<FileInfoRepository, Mono<FileInfo>> getter) {
        return findRepositorySingle(r -> r.isSupported(id), getter);
    }

    /**
     * 按优先级查找支持过滤器且获取文件信息成功的第一个存储方式及对应文件信息
     *
     * @param filter 过滤器
     * @param getter 获取文件信息的函数
     * @return 文件信息
     */
    public Mono<Tuple2<FileInfoRepository, FileInfo>> findRepositorySingle(Predicate<FileInfoRepository> filter,
                                                                           Function<FileInfoRepository, Mono<FileInfo>> getter) {
        Lazy<BusinessException> lazyError = Lazy.of(() -> new BusinessException.NoStackTrace("error.file_not_found"));

        return Flux
            .fromIterable(repositoryList)
            .concatMap(r -> Flux
                .defer(() -> {
                    if (filter.test(r)) {
                        return getter
                            .apply(r)
                            .map(info -> Tuples.of(r, info));
                    }
                    return Mono.empty();
                })
                .onErrorResume(err -> Mono.fromRunnable(() -> lazyError.get().addSuppressed(err)))
            )
            .take(1)
            .singleOrEmpty()
            .switchIfEmpty(Mono.error(lazyError));
    }


    /**
     * 按优先级查找支持指定文件id的存储方式获取文件信息
     *
     * @param id 文件id
     * @return 文件存储方式
     */
    public Mono<FileInfo> findFileInfoSingle(String id,
                                             Function<FileInfoRepository, Mono<FileInfo>> getter) {
        return findRepositorySingle(id, getter).map(Tuple2::getT2);
    }

    /**
     * 按优先级查找支持过滤器的存储方式获取文件信息
     *
     * @param filter 过滤器
     * @param getter 获取文件信息的函数
     * @return 文件信息
     */
    public Mono<FileInfo> findFileInfoSingle(Predicate<FileInfoRepository> filter,
                                             Function<FileInfoRepository, Mono<FileInfo>> getter) {
        return findRepositorySingle(filter, getter).map(Tuple2::getT2);
    }


    /**
     * 遍历所有存储方式，获取并返回所有文件信息
     *
     * @param getter        获取函数
     * @param consumerError 错误处理
     * @return 文件信息
     */
    public Flux<FileInfo> walkRepository(Function<FileInfoRepository, ? extends Publisher<FileInfo>> getter,
                                         Consumer<Throwable> consumerError) {
        return Flux
            .fromIterable(repositoryList)
            .concatMap(r -> Flux
                .defer(() -> getter.apply(r))
                .onErrorResume(err -> Mono.fromRunnable(() -> consumerError.accept(err)))
            );
    }


}
