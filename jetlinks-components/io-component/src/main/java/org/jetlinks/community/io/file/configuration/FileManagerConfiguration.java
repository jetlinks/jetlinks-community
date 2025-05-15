package org.jetlinks.community.io.file.configuration;

import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.jetlinks.community.io.file.FileEntityService;
import org.jetlinks.community.io.file.FileProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(FileProperties.class)
@EnableEasyormRepository("org.jetlinks.community.io.file.FileEntity")
public class FileManagerConfiguration {

    @Bean
    public FileEntityService fileEntityService() {
        return new FileEntityService();
    }



}
