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
package org.jetlinks.community.io.file.configuration;

import org.hswebframework.web.file.FileServiceConfiguration;
import org.hswebframework.web.file.service.FileStorageService;
import org.jetlinks.community.config.ConfigManager;
import org.jetlinks.community.io.file.*;
import org.jetlinks.community.io.file.FileCommandSupportManager;
import org.jetlinks.community.io.file.repository.DatabaseFileInfoRepository;
import org.jetlinks.community.io.file.repository.FileInfoRepository;
import org.jetlinks.community.io.file.repository.FileInfoRepositoryHelper;
import org.jetlinks.community.io.file.service.LocalFileServiceProvider;
import org.jetlinks.community.io.file.service.FileServiceProvider;
import org.jetlinks.core.rpc.RpcManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
public class DefaultFileManagerConfiguration {

    @Bean
    public LocalFileServiceProvider clusterFileServiceProvider(RpcManager rpcManager,
                                                               FileProperties properties,
                                                               ReactiveRedisTemplate<String, String> template) {
        LocalFileServiceProvider provider = new LocalFileServiceProvider(rpcManager, properties, template);
        FileServiceProvider.providers.register(provider.getType(), provider);
        return provider;
    }

    @Bean
    @ConditionalOnMissingBean(FileManager.class)
    public FileManager fileManager(FileProperties properties, ConfigManager configManager, FileInfoRepositoryHelper fileInfoRepositoryHelper) {
        return new DefaultFileManager(properties, configManager, fileInfoRepositoryHelper);
    }

    @Bean
    public FileInfoRepositoryHelper fileInfoRepositoryHelper(ObjectProvider<FileInfoRepository> repositories) {
        return new FileInfoRepositoryHelper(repositories);
    }

    @Bean
    public DatabaseFileInfoRepository databaseFileInfoRepository(FileEntityService service) {
        return new DatabaseFileInfoRepository(service);
    }

    @Bean
    public FileEntityEventHandler fileEntityEventHandler(FileManager fileManager) {
        return new FileEntityEventHandler(fileManager);
    }


    @Bean
    public FileCommandSupportManager fileCommandSupportManager(WebClient.Builder builder,
                                                               FileManager fileManager) {
        return new FileCommandSupportManager(builder, fileManager);
    }

    @AutoConfiguration(before = FileServiceConfiguration.class)
    @ConditionalOnClass(FileStorageService.class)
    static class StorageServiceConfiguration {

        @Bean
        public FileStorageService fileStorageService(FileManager fileManager) {
            return new FileManagerStorageService(fileManager);
        }
    }
}
