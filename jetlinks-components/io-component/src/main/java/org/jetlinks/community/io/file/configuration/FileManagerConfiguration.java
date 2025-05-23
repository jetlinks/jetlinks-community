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
