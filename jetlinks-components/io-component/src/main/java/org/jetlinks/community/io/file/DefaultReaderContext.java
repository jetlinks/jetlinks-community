package org.jetlinks.community.io.file;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DefaultReaderContext implements FileManager.ReaderContext {
    private final FileInfo info;
    private long position;
    private long length;

    @Override
    public FileInfo info() {
        return info;
    }

    @Override
    public void position(long position) {
        this.position = position;
    }

    @Override
    public void length(long length) {
        this.length = length;
    }
}