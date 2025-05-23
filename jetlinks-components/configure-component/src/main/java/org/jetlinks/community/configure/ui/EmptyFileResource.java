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
package org.jetlinks.community.configure.ui;

import lombok.AllArgsConstructor;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

@AllArgsConstructor
class EmptyFileResource extends AbstractResource {

    private final Resource resource;

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    @Nonnull
    public String getDescription() {
        return resource.getDescription();
    }

    @Override
    public long lastModified() throws IOException {
        return resource.exists() ? resource.lastModified() : -1;
    }

    @Override
    public long contentLength() throws IOException {
        return 0;
    }

    @Override
    public String getFilename() {
        return resource.getFilename();
    }

    @Override
    @Nonnull
    public File getFile() throws IOException {
        return resource.getFile();
    }

    @Override
    @Nonnull
    public URL getURL() throws IOException {
        return resource.getURL();
    }

    @Override
    @Nonnull
    public URI getURI() throws IOException {
        return resource.getURI();
    }

    @Override
    @Nonnull
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(new byte[0]);
    }
}
