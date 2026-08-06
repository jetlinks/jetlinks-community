/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.io.excel;

import org.jetlinks.community.io.file.FileManager;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultImportExportServiceTest {

    @Test
    void shouldReadManagedFileInsteadOfRemoteUrl() {
        FileManager fileManager = mock(FileManager.class);
        when(fileManager.read("file-id")).thenReturn(Flux.empty());

        DefaultImportExportService service = new DefaultImportExportService(fileManager);

        StepVerifier
            .create(service.getInputStream("http://localhost:8848/api/file/file-id.csv?accessKey=test"))
            .verifyComplete();

        verify(fileManager).read("file-id");
    }

    @Test
    void shouldResolveManagedFileId() {
        assertEquals("file-id", DefaultImportExportService.resolveFileId("file-id"));
        assertEquals(
            "file-id",
            DefaultImportExportService.resolveFileId("http://localhost:8848/api/file/file-id.xlsx?accessKey=test")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> DefaultImportExportService.resolveFileId("http://127.0.0.1/internal/secret.csv")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> DefaultImportExportService.resolveFileId("/etc/passwd")
        );
    }
}
