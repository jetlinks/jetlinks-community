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
package org.jetlinks.community.io.file;

import org.hswebframework.web.crud.service.GenericReactiveCacheSupportCrudService;
import org.springframework.boot.CommandLineRunner;

public class FileEntityService extends GenericReactiveCacheSupportCrudService<FileEntity,String> implements CommandLineRunner {

    @Override
    public String getCacheName() {
        return "file-manager";
    }


    @Override
    public void run(String... args) throws Exception {
        //兼容FileEntity结构变更,否则序列化会冲突
        getCache()
            .clear()
            .subscribe();
    }
}
