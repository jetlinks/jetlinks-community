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
