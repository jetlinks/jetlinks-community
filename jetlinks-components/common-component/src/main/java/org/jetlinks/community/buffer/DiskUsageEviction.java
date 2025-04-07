package org.jetlinks.community.buffer;


import java.io.File;
import java.util.Map;

class DiskUsageEviction extends AbstractBufferEviction {

    private final File path;
    private final float threshold;

    public DiskUsageEviction(File path, float threshold) {
        this.path = path;
        this.threshold = threshold;
    }

    private volatile float usage;
    private volatile long lastUpdateTime;

    @Override
    public boolean doEviction(EvictionContext context) {
        tryUpdate();

        if (freeOutOfThreshold()) {
            context.removeOldest(EvictionContext.BufferType.buffer);
            return true;
        }
        return false;
    }

    protected boolean freeOutOfThreshold() {
        return usage >= threshold;
    }

    private void tryUpdate() {
        long now = System.currentTimeMillis();
        //1秒更新一次
        if (now - lastUpdateTime <= 1000) {
            return;
        }
        long total = path.getTotalSpace();
        long usable = path.getUsableSpace();

        usage = (float) ((total - usable) / (double) total);
        lastUpdateTime = now;
    }

    @Override
    protected void applyEventData(Map<String, Object> data) {
        data.put("usage", String.format("%.2f%%", usage * 100));
    }

    @Override
    public String toString() {
        return "DiskUsage(path=" + path
            + ", threshold=" + String.format("%.2f%%", threshold * 100)
            + ", usage=" + String.format("%.2f%%", usage * 100) + ")";
    }
}
