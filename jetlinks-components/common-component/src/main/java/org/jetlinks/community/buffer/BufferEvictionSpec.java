package org.jetlinks.community.buffer;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
public class BufferEvictionSpec {

    public static final BufferEviction DEFAULT = new BufferEvictionSpec().build();

    //最大队列数量,超过则淘汰最旧的数据
    private int maxSize = -1;

    //最大死信数量,超过则淘汰dead数据
    private int maxDeadSize = Integer.getInteger("jetlinks.buffer.dead.limit", 100_0000);

    //根据磁盘空间淘汰数据
    private DataSize diskFree = DataSize.parse(System.getProperty("jetlinks.buffer.disk.free.threshold", "4GB"));

    //磁盘最大使用率
    private float diskThreshold;

    //判断磁盘空间大小的目录
    private String diskPath = System.getProperty("jetlinks.buffer.disk.free.path", "./");

    public BufferEviction build() {

        BufferEviction
            eviction = null,
            size = BufferEviction.limit(maxSize, maxDeadSize),
            disk = null;

        if (diskThreshold > 0) {
            disk = BufferEviction.disk(diskPath, diskThreshold);
        } else if (diskFree != null) {
            disk = BufferEviction.disk(diskPath, diskFree);
        }

        if (disk != null) {
            eviction = disk;
        }

        if (eviction == null) {
            eviction = size;
        } else {
            eviction = eviction.then(size);
        }

        return eviction;
    }


}
