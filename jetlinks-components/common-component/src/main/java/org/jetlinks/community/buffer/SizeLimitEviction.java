package org.jetlinks.community.buffer;

import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
class SizeLimitEviction extends AbstractBufferEviction {

    private final long bufferLimit;
    private final long deadLimit;

    @Override
    public boolean doEviction(EvictionContext context) {
        boolean anyEviction = false;
        if (bufferLimit > 0 && context.size(EvictionContext.BufferType.buffer) >= bufferLimit) {
            context.removeOldest(EvictionContext.BufferType.buffer);
            anyEviction = true;
        }
        if (deadLimit > 0 && context.size(EvictionContext.BufferType.dead) >= deadLimit) {
            context.removeOldest(EvictionContext.BufferType.dead);
            anyEviction = true;
        }
        return anyEviction;
    }

    @Override
    protected void applyEventData(Map<String, Object> data) {
        data.put("bufferLimit", bufferLimit);
        data.put("deadLimit", deadLimit);
    }

    @Override
    public String toString() {
        return "SizeLimit(buffer=" + bufferLimit + ", dead=" + deadLimit + ")";
    }
}
