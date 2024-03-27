package org.jetlinks.community.io.file;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.hswebframework.web.file.FileServiceConfiguration;
import org.hswebframework.web.file.service.FileStorageService;
import org.jetlinks.community.config.ConfigManager;
import org.jetlinks.core.rpc.RpcManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileProperties.class)
@EnableEasyormRepository("org.jetlinks.community.io.file.FileEntity")
public class FileManagerConfiguration {


    @Bean
    public FileManager fileManager(RpcManager rpcManager,
                                   FileProperties properties,
                                   ReactiveRepository<FileEntity, String> repository,
                                   ConfigManager configManager){
        return new ClusterFileManager(rpcManager,properties,repository,configManager);
    }

    @AutoConfiguration(before = FileServiceConfiguration.class)
    @ConditionalOnClass(FileStorageService.class)
    static class StorageServiceConfiguration{

        @Bean
        public FileStorageService fileStorageService(FileManager fileManager) {
            return new FileManagerStorageService(fileManager);
        }
    }

}
