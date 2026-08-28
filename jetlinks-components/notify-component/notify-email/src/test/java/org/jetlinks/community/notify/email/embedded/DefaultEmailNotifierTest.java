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
package org.jetlinks.community.notify.email.embedded;

import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.notify.template.TemplateManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultEmailNotifierTest {

    @Test
    void shouldRejectRemoteAttachment() {
        FileManager fileManager = mock(FileManager.class);
        DefaultEmailNotifier notifier = createNotifier(fileManager);

        StepVerifier
            .create(notifier.convertResource("http://127.0.0.1/internal"))
            .expectError(UnsupportedOperationException.class)
            .verify();

        verifyNoInteractions(fileManager);
    }

    @Test
    void shouldReadManagedAttachmentUrl() {
        byte[] content = "managed-attachment".getBytes(StandardCharsets.UTF_8);
        FileManager fileManager = mock(FileManager.class);
        when(fileManager.read("file-id"))
            .thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(content)));

        DefaultEmailNotifier notifier = createNotifier(fileManager);

        StepVerifier
            .create(notifier
                        .convertResource("http://localhost:8848/api/file/file-id.txt?accessKey=test")
                        .map(DefaultEmailNotifierTest::readAllBytes))
            .assertNext(actual -> assertArrayEquals(content, actual))
            .verifyComplete();

        verify(fileManager).read("file-id");
    }

    @Test
    void shouldKeepExplicitLocalAttachment() {
        FileManager fileManager = mock(FileManager.class);
        DefaultEmailNotifier notifier = createNotifier(fileManager);
        notifier.setEnableFileSystemAttachment(true);

        StepVerifier
            .create(notifier.convertResource("/tmp/attachment.txt"))
            .assertNext(resource -> {
                FileSystemResource local = assertInstanceOf(FileSystemResource.class, resource);
                assertEquals("/tmp/attachment.txt", local.getPath());
            })
            .verifyComplete();

        verifyNoInteractions(fileManager);
    }

    @Test
    void shouldKeepBase64Attachment() {
        byte[] content = "base64-attachment".getBytes(StandardCharsets.UTF_8);
        FileManager fileManager = mock(FileManager.class);
        DefaultEmailNotifier notifier = createNotifier(fileManager);

        StepVerifier
            .create(notifier
                        .convertResource("data:text/plain;base64," + Base64.getEncoder().encodeToString(content))
                        .map(DefaultEmailNotifierTest::readAllBytes))
            .assertNext(actual -> assertArrayEquals(content, actual))
            .verifyComplete();

        verifyNoInteractions(fileManager);
    }

    private static DefaultEmailNotifier createNotifier(FileManager fileManager) {
        DefaultEmailProperties properties = new DefaultEmailProperties();
        properties.setHost("localhost");
        properties.setPort(25);
        properties.setSender("test@example.com");
        return new DefaultEmailNotifier(
            "test",
            properties,
            mock(TemplateManager.class),
            fileManager,
            WebClient.builder()
        );
    }

    private static byte[] readAllBytes(InputStreamSource resource) {
        try (var stream = resource.getInputStream()) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
