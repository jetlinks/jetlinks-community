package org.jetlinks.community.io.file;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
public class FileEntityEventHandler {

    private final FileManager fileManager;

    /**
     * 用于兼容旧版本，建议使用{@link FileManager#delete(String)}删除文件，而不是直接删除实体
     */
    @EventListener
    public void handleDeleteEvent(EntityDeletedEvent<FileEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(file -> fileManager
                    .delete(file.getId())
                    .onErrorResume(err -> {
                        log.error("delete file [{}] error", file.getId(), err);
                        return Mono.empty();
                    }))
        );
    }
}
