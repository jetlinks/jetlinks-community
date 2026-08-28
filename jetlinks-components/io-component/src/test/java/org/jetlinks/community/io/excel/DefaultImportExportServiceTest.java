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

import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.io.utils.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultImportExportServiceTest {

    @Test
    void shouldReadManagedFileInsteadOfRemoteUrl() {
        byte[] content = "managed-file".getBytes(StandardCharsets.UTF_8);
        FileManager fileManager = mock(FileManager.class);
        when(fileManager.read("file-id"))
            .thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(content)));

        DefaultImportExportService service = new DefaultImportExportService(fileManager);

        StepVerifier
            .create(service
                        .getInputStream("http://localhost:8848/api/file/file-id.csv?accessKey=test")
                        .map(DefaultImportExportServiceTest::readAllBytes))
            .assertNext(actual -> assertArrayEquals(content, actual))
            .verifyComplete();

        verify(fileManager).read("file-id");
    }

    @Test
    void shouldRejectExternalOrLocalFileBeforeReading() {
        FileManager fileManager = mock(FileManager.class);
        DefaultImportExportService service = new DefaultImportExportService(fileManager);

        StepVerifier
            .create(service.getInputStream("http://127.0.0.1/internal/secret.csv"))
            .expectError(ValidationException.class)
            .verify();
        StepVerifier
            .create(service.getInputStream("/etc/passwd"))
            .expectError(ValidationException.class)
            .verify();

        verifyNoInteractions(fileManager);
    }

    @Test
    void shouldResolveManagedFileId() {
        assertEquals("file-id", FileUtils.resolveManagedFileId("file-id"));
        assertEquals(
            "file-id",
            FileUtils.resolveManagedFileId("http://localhost:8848/api/file/file-id.xlsx?accessKey=test")
        );
        assertEquals(
            "file-id",
            FileUtils.resolveManagedFileId("/api/file/file-id.xlsx?accessKey=test")
        );
        assertThrows(
            ValidationException.class,
            () -> FileUtils.resolveManagedFileId("http://127.0.0.1/internal/secret.csv")
        );
        assertThrows(
            ValidationException.class,
            () -> FileUtils.resolveManagedFileId("/etc/passwd")
        );
        assertThrows(
            ValidationException.class,
            () -> FileUtils.resolveManagedFileId("file:///etc/passwd")
        );
        assertThrows(
            ValidationException.class,
            () -> FileUtils.resolveManagedFileId(" ")
        );
        assertThrows(
            ValidationException.class,
            () -> FileUtils.resolveManagedFileId("/api/file/.xlsx")
        );
        assertThrows(
            ValidationException.class,
            () -> FileUtils.resolveManagedFileId("/api/file/")
        );
        assertThrows(
            ValidationException.class,
            () -> FileUtils.resolveManagedFileId("/api/file/file-id/extra.xlsx")
        );
        assertThrows(
            ValidationException.class,
            () -> FileUtils.resolveManagedFileId("file-id?accessKey=test")
        );
        assertThrows(
            ValidationException.class,
            () -> FileUtils.resolveManagedFileId("http://[::1")
        );
    }

    private static byte[] readAllBytes(InputStream stream) {
        try (stream) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
