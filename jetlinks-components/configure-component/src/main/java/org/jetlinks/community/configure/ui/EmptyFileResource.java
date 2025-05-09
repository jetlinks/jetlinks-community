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
