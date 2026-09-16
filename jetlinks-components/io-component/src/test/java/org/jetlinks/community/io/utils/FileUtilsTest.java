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
package org.jetlinks.community.io.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsTest {

    @Test
    void testSafeResponseMediaTypeAndActiveContent() {
        assertFalse(FileUtils.isActiveContentExtension(null));
        assertFalse(FileUtils.isActiveContentExtension(""));
        assertFalse(FileUtils.isActiveContentExtension("  "));
        assertFalse(FileUtils.isActiveContentExtension("jpg"));
        assertFalse(FileUtils.isActiveContentExtension("json"));
        assertFalse(FileUtils.isScriptableDocumentExtension("svg"));
        assertFalse(FileUtils.isScriptableDocumentExtension("svgz"));

        assertTrue(FileUtils.isActiveContentExtension("HTML"));
        assertTrue(FileUtils.isActiveContentExtension(".html"));
        assertTrue(FileUtils.isActiveContentExtension(" HTML "));
        assertTrue(FileUtils.isActiveContentExtension("svg"));
        assertTrue(FileUtils.isActiveContentExtension("JS"));
        assertTrue(FileUtils.isActiveContentExtension("xml"));
        assertTrue(FileUtils.isActiveContentExtension("shtml"));
        assertTrue(FileUtils.isActiveContentExtension("cjs"));
        assertTrue(FileUtils.isScriptableDocumentExtension("htm"));
        assertTrue(FileUtils.isScriptableDocumentExtension("xhtml"));
        assertTrue(FileUtils.isScriptableDocumentExtension("mjs"));
        assertTrue(FileUtils.isScriptableDocumentExtension("xslt"));
        assertTrue(FileUtils.isScriptableDocumentExtension("xsl"));

        assertEquals(MediaType.TEXT_HTML, FileUtils.getMediaTypeByExtension("html"));
        assertEquals(MediaType.parseMediaType("text/javascript"), FileUtils.getMediaTypeByExtension("js"));
        assertEquals(MediaType.TEXT_XML, FileUtils.getMediaTypeByExtension("xml"));
        assertEquals(MediaType.parseMediaType("image/svg+xml"), FileUtils.getMediaTypeByExtension("svg"));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM,
                     FileUtils.safeResponseMediaType("html", MediaType.TEXT_HTML));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM,
                     FileUtils.safeResponseMediaType("HTM", MediaType.TEXT_HTML));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM,
                     FileUtils.safeResponseMediaType(".js", MediaType.parseMediaType("text/javascript")));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM,
                     FileUtils.safeResponseMediaType("xml", MediaType.TEXT_XML));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM,
                     FileUtils.safeResponseMediaType("shtml", MediaType.TEXT_HTML));

        MediaType svg = MediaType.parseMediaType("image/svg+xml");
        assertEquals(svg, FileUtils.safeResponseMediaType("svg", svg));
        assertEquals(svg, FileUtils.safeResponseMediaType("svgz", MediaType.APPLICATION_OCTET_STREAM));
        assertEquals(svg, FileUtils.safeResponseMediaType("svg", MediaType.TEXT_HTML));
        assertEquals(svg, FileUtils.safeResponseMediaType("svgz", MediaType.TEXT_XML));

        assertEquals(MediaType.IMAGE_JPEG, FileUtils.safeResponseMediaType("jpg", MediaType.IMAGE_JPEG));
        assertEquals(MediaType.APPLICATION_JSON, FileUtils.safeResponseMediaType("json", MediaType.APPLICATION_JSON));

        assertTrue(FileUtils.shouldForceAttachment("html", false, MediaType.APPLICATION_OCTET_STREAM));
        assertTrue(FileUtils.shouldForceAttachment("svg", false, svg));
        assertTrue(FileUtils.shouldForceAttachment("jpg", true, MediaType.IMAGE_JPEG));
        assertTrue(FileUtils.shouldForceAttachment("bin", false, MediaType.APPLICATION_OCTET_STREAM));
        assertFalse(FileUtils.shouldForceAttachment("jpg", false, MediaType.IMAGE_JPEG));
        assertFalse(FileUtils.shouldForceAttachment("json", false, MediaType.APPLICATION_JSON));
        assertFalse(FileUtils.shouldForceAttachment("txt", false, MediaType.TEXT_PLAIN));
    }
}
