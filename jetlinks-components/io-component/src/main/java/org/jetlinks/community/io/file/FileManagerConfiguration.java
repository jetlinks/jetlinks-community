package org.jetlinks.community.io.file;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(FileProperties.class)
@EnableEasyormRepository("org.jetlinks.community.io.file.FileEntity")
public class FileManagerConfiguration {


    @Bean
    public FileManager fileManager(WebClient.Builder builder,
                                   FileProperties properties,
                                   ReactiveRepository<FileEntity, String> repository){
        return new DefaultFileManager(builder,properties,repository);
    }

}
