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
