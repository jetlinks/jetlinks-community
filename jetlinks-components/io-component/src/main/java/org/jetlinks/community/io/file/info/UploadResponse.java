package org.jetlinks.community.io.file.info;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.io.file.FileInfo;
import org.jetlinks.community.io.file.FileOption;

import java.io.Serializable;
import java.util.*;

/**
 * @author gyl
 * @since 2.3
 */
@Getter
@Setter
public class UploadResponse implements Serializable {

    //文件相对路径
    private String path;
    private long length;
    private String md5;
    private String sha256;
    //所在服务id
    private String serverNodeId;

    private FileOption[] options;
    private Map<String, Object> others;
    private String accessUrl;

    public synchronized UploadResponse withOther(String key, Object value) {
        if (others == null) {
            others = new HashMap<>();
        }
        others.put(key, value);
        return this;
    }

    public FileInfo toShardingInfo(FileInfo base) {
        FileInfo copy = toInfo(base);
        copy.setPath(path);
        return copy;
    }

    public FileInfo toInfo(FileInfo base) {
        FileInfo copy = toInfo0(base);
        copy.setLength(length);
        return copy;
    }

    public FileInfo toInfo0(FileInfo base) {
        FileInfo copy = FastBeanCopier.copy(base, new FileInfo());
        copy.setServerNodeId(serverNodeId);
        copy.setAccessUrl(accessUrl);
        copy.setMd5(md5);
        copy.setSha256(sha256);

        List<FileOption> all = new ArrayList<>();
        if (options != null) {
            Collections.addAll(all, options);
        }
        if (base.getOptions() != null) {
            Collections.addAll(all, base.getOptions());
        }
        copy.setOptions(all.toArray(new FileOption[0]));

        if (copy.getOthers() == null) {
            copy.setOthers(others);
        } else if (others != null) {
            copy.getOthers().putAll(others);
        }
        return copy;
    }
}
