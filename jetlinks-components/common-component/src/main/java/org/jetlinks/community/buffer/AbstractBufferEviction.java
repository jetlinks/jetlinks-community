package org.jetlinks.community.buffer;

import org.jetlinks.community.Operation;
import org.jetlinks.community.OperationSource;
import org.jetlinks.community.OperationType;
import org.jetlinks.community.event.SystemEventHolder;
import org.jetlinks.community.utils.TimeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

public abstract class AbstractBufferEviction implements BufferEviction {

    public static final OperationType OPERATION_TYPE = OperationType.of("buffer-eviction", "缓冲区数据丢弃");

    private static final AtomicLongFieldUpdater<AbstractBufferEviction>
        LAST_EVENT_TIME = AtomicLongFieldUpdater.newUpdater(AbstractBufferEviction.class, "lastEventTime");
    private static final AtomicIntegerFieldUpdater<AbstractBufferEviction>
        LAST_TIMES = AtomicIntegerFieldUpdater.newUpdater(AbstractBufferEviction.class, "lastTimes");

    //最大事件推送频率
    //可通过java -Djetlinks.buffer.eviction.event.max-interval=10m修改配置
    private static final long MAX_EVENT_INTERVAL =
        TimeUtils.parse(System.getProperty("jetlinks.buffer.eviction.event.max-interval", "10m")).toMillis();

    private volatile long lastEventTime;
    private volatile int lastTimes;

    abstract boolean doEviction(EvictionContext context);

    @Override
    public boolean tryEviction(EvictionContext context) {
        if (doEviction(context)) {
            sendEvent(context);
            return true;
        }
        return false;
    }

    private String operationCode() {
        return getClass().getSimpleName();
    }

    private void sendEvent(EvictionContext context) {
        long now = System.currentTimeMillis();
        long time = LAST_EVENT_TIME.get(this);
        //记录事件推送周期内总共触发了多少次
        LAST_TIMES.incrementAndGet(this);

        //超过间隔事件则推送事件,防止推送太多错误事件
        if (now - time > MAX_EVENT_INTERVAL) {
            LAST_EVENT_TIME.set(this, now);
            Map<String, Object> info = new HashMap<>();

            //缓冲区数量
            info.put("bufferSize", context.size(EvictionContext.BufferType.buffer));
            //死数据数量
            info.put("deadSize", context.size(EvictionContext.BufferType.dead));
            //总计触发次数
            info.put("times", LAST_TIMES.getAndSet(this, 0));

            //应用自定义的数据,比如磁盘剩余空间等信息
            applyEventData(info);

            //推送系统事件
            SystemEventHolder.warn(
                Operation.of(
                    OperationSource.of(context.getName(), "eviction"),
                    OPERATION_TYPE),
                operationCode(),
                info
            );
        }
    }

    protected void applyEventData(Map<String, Object> data) {

    }
}
