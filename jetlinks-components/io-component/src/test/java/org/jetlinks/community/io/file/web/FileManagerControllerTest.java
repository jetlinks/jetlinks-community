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
package org.jetlinks.community.io.file.web;

import org.jetlinks.community.io.file.FileInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileManagerControllerTest {

    @Test
    void shouldServeHtmlAsOctetStreamAttachmentWithSandbox() {
        HttpHeaders headers = apply("evil.html", false);

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, headers.getContentType());
        assertTrue(disposition(headers).toLowerCase().contains("attachment"));
        assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
        assertTrue(headers.getFirst("Content-Security-Policy").contains("sandbox"));
    }

    @Test
    void shouldKeepSvgImageTypeWithAttachmentAndSandbox() {
        HttpHeaders headers = apply("icon.svg", false);

        assertEquals(MediaType.parseMediaType("image/svg+xml"), headers.getContentType());
        assertTrue(disposition(headers).toLowerCase().contains("attachment"));
        assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
        assertTrue(headers.getFirst("Content-Security-Policy").contains("sandbox"));
    }

    @Test
    void shouldServeJsAndXmlAsOctetStreamAttachmentWithSandbox() {
        HttpHeaders js = apply("app.js", false);
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, js.getContentType());
        assertTrue(disposition(js).toLowerCase().contains("attachment"));
        assertEquals("nosniff", js.getFirst("X-Content-Type-Options"));
        assertTrue(js.getFirst("Content-Security-Policy").contains("sandbox"));

        HttpHeaders xml = apply("data.xml", false);
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, xml.getContentType());
        assertTrue(disposition(xml).toLowerCase().contains("attachment"));
        assertEquals("nosniff", xml.getFirst("X-Content-Type-Options"));
        assertTrue(xml.getFirst("Content-Security-Policy").contains("sandbox"));
    }

    @Test
    void shouldNotForceAttachmentForJpegOrJson() {
        HttpHeaders jpeg = apply("photo.jpg", false);
        assertEquals(MediaType.IMAGE_JPEG, jpeg.getContentType());
        assertNull(jpeg.getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("nosniff", jpeg.getFirst("X-Content-Type-Options"));
        assertNull(jpeg.getFirst("Content-Security-Policy"));

        HttpHeaders json = apply("data.json", false);
        assertEquals(MediaType.APPLICATION_JSON, json.getContentType());
        assertNull(json.getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("nosniff", json.getFirst("X-Content-Type-Options"));
        assertNull(json.getFirst("Content-Security-Policy"));
    }

    @Test
    void shouldKeepExplicitAttachmentForSafeTypes() {
        HttpHeaders headers = apply("photo.jpg", true);

        assertEquals(MediaType.IMAGE_JPEG, headers.getContentType());
        assertTrue(disposition(headers).toLowerCase().contains("attachment"));
        assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
        assertNull(headers.getFirst("Content-Security-Policy"));
    }

    @Test
    void shouldSetHtmlAttachmentEvenWhenCalledIndependentlyOfRangeLogic() {
        HttpHeaders headers = new HttpHeaders();
        FileManagerController.applySecurityReadHeaders(fileInfo("range.html"), headers, false);

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, headers.getContentType());
        assertTrue(disposition(headers).toLowerCase().contains("attachment"));
        assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
        assertTrue(headers.getFirst("Content-Security-Policy").contains("sandbox"));
        assertFalse(headers.containsKey(HttpHeaders.CONTENT_RANGE));
        assertFalse(headers.containsKey(HttpHeaders.ACCEPT_RANGES));
    }

    private static HttpHeaders apply(String fileName, boolean attachment) {
        HttpHeaders headers = new HttpHeaders();
        FileManagerController.applySecurityReadHeaders(fileInfo(fileName), headers, attachment);
        return headers;
    }

    private static FileInfo fileInfo(String fileName) {
        return new FileInfo().withFileName(fileName);
    }

    private static String disposition(HttpHeaders headers) {
        String value = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        return value == null ? "" : value;
    }
}
