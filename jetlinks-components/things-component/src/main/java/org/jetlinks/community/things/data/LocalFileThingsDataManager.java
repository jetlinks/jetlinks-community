package org.jetlinks.community.things.data;

import io.netty.buffer.*;
import io.netty.util.ReferenceCountUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jetlinks.community.codec.Serializers;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.core.message.property.SimplePropertyValue;
import org.jetlinks.core.things.ThingEvent;
import org.jetlinks.core.things.ThingProperty;
import org.jetlinks.core.things.ThingTag;
import org.jetlinks.core.things.ThingsDataManager;
import org.jetlinks.core.utils.NumberUtils;
import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.supports.utils.MVStoreUtils;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.function.Function4;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class LocalFileThingsDataManager implements ThingsDataManager, ThingsDataWriter {

    private static final AtomicIntegerFieldUpdater<LocalFileThingsDataManager>
        TAG_INC = AtomicIntegerFieldUpdater.newUpdater(LocalFileThingsDataManager.class, "tagInc");

    //单个属性最大缓存数量 java -Dthings.data.store.max-size=8
    static int DEFAULT_MAX_STORE_SIZE_EACH_KEY = Integer
        .parseInt(
            System.getProperty("jetlinks.things.data.store.max-size", "8")
        );

    static Duration FLUSH_INTERVAL = TimeUtils
        .parse(
            System.getProperty("jetlinks.things.data.store.flush-interval", "30s")
        );

    static int CACHE_SIZE = (int) Math.max(64, Runtime.getRuntime().maxMemory() / 1024 / 1024 / 64);

    protected final MVStore mvStore;

    //记录key的标签缓存,此方式决定了支持的最大(物实例数量+属性数量)为2^32(42亿)
    private final Map<String, Integer> tagCache = new ConcurrentHashMap<>();
    private final MVMap<String, Integer> tagStore;

    //  历史数据缓存
    // key为什么不直接使用Long?因为tag的生成规则会导致hash冲突严重.(同一个物实例的所有属性tag hash值一样)
    private final Map<StoreKey, PropertyHistory> l1Cache = new ConcurrentHashMap<>();
    private final MVMap<Long, PropertyHistory> historyStore;
    private final Scheduler writerScheduler = Schedulers.newSingle("things-data-writer");
    private final Scheduler readerScheduler = Schedulers.newSingle("things-data-reader");
    private final Disposable.Composite disposable = Disposables.composite();
    private volatile int tagInc;

    public LocalFileThingsDataManager(String fileName) {

        Tuple3<MVStore,
            MVMap<String, Integer>,
            MVMap<Long, PropertyHistory>> tp3 = MVStoreUtils
            .open(new File(fileName),
                  "things-data-manager",
                  c -> c.keysPerPage(1024)
                        .cacheSize(CACHE_SIZE),
                  store -> Tuples.of(
                      store,
                      MVStoreUtils.openMap(store, "tags", new MVMap.Builder<>()),
                      MVStoreUtils.openMap(store, "store", new MVMap
                          .Builder<Long, PropertyHistory>()
                          .valueType(new HistoryType()))));

        this.mvStore = tp3.getT1();
        this.tagStore = tp3.getT2();
        this.historyStore = tp3.getT3();
        this.tagInc = this.tagStore.size();
        init();
    }

    public void shutdown() {
        flushNow();
        mvStore.close(10_000);
        this.disposable.dispose();
    }

    private void init() {
        disposable.add(
            Flux.interval(FLUSH_INTERVAL)
                .onBackpressureDrop(dropped -> log.info("flush thing data too slow! in memory size:{}", l1Cache.size()))
                .concatMap(ignore -> flushAsync(), 1)
                .subscribe());
        disposable.add(this.writerScheduler);
        disposable.add(this.readerScheduler);
    }

    static final MVMap.DecisionMaker<PropertyHistory> MERGE = new MVMap.DecisionMaker<PropertyHistory>() {
        @Override
        public MVMap.Decision decide(PropertyHistory existingValue, PropertyHistory providedValue) {
            if (existingValue != null && providedValue.isDirty()) {
                providedValue
                    .merge(existingValue)
                    .setDirty(false);
            }
            providedValue.setStored(true);
            return MVMap.Decision.PUT;
        }
    };

    synchronized void flushNow() {
        long ms = System.currentTimeMillis();
        log.info("flushing thing data, in memory size:{}", l1Cache.size());
        for (Map.Entry<StoreKey, PropertyHistory> entry : l1Cache.entrySet()) {
            StoreKey key = entry.getKey();
            PropertyHistory history = entry.getValue();
            if (!history.isStored()) {
                historyStore.operate(key.toTag(), history, MERGE);
            }
            //上一次被标记为空闲,则本次移除
            if (history.isIdle()) {
                l1Cache.remove(key);
            }
            //标记为空闲
            else {
                history.setIdle(true);
            }
        }
        log.info("flushing thing data complete {}ms, in memory size:{}", System.currentTimeMillis() - ms, l1Cache.size());
    }

    protected Mono<Void> flushAsync() {
        return Mono
            .<Void>fromRunnable(this::flushNow)
            .subscribeOn(writerScheduler)
            .onErrorResume(err -> {
                log.warn("write thing data error. in memory size:{}", l1Cache.size(), err);
                return Mono.empty();
            });
    }

    @Override
    public Mono<ThingProperty> getLastProperty(String thingType,
                                               String thingId,
                                               String property,
                                               long baseTime) {
        return this.getHistory(
            thingType,
            thingId,
            property,
            prop -> {
                Property p = prop.getProperty(baseTime);
                if (p == null) {
                    return null;
                }
                return p.toProperty(property);
            });

    }

    protected Mono<ThingProperty> lastPropertyNotFound(String thingType,
                                                       String thingId,
                                                       String property,
                                                       long baseTime) {
        return Mono.empty();
    }

    protected Mono<ThingProperty> firstPropertyNotFound(String thingType,
                                                        String thingId,
                                                        String property) {
        return Mono.empty();
    }

    @Override
    public Mono<ThingProperty> getFirstProperty(String thingType,
                                                String thingId,
                                                String property) {
        return this.getHistory(
            thingType,
            thingId,
            property,
            prop -> {
                Property p = prop.first;
                if (p == null) {
                    return null;
                }
                return p.toProperty(property);
            });
    }

    @Override
    public Mono<List<ThingProperty>> getProperties(String thingType,
                                                   String thingId,
                                                   String property,
                                                   long baseTime) {
        return this.getProperties(thingType,
                                  thingId,
                                  property,
                                  0,
                                  baseTime);
    }

    @Override
    public Mono<List<ThingProperty>> getProperties(String thingType,
                                                   String thingId,
                                                   String property,
                                                   long from,
                                                   long to) {
        return this.getHistory(
            thingType,
            thingId,
            property,
            prop -> prop.getProperties(property, from, to));
    }

    protected <T> Mono<T> getHistory(String thingType,
                                     String thingId,
                                     String property,
                                     Function<PropertyHistory, T> mapper) {
        StoreKey key = getPropertyStoreKeyObj(thingType, thingId, property);
        PropertyHistory his = l1Cache.get(key);
        //fast path
        if (his != null && !his.isDirty()) {
            return Mono.justOrEmpty(mapper.apply(his));
        }
        // slow path
        return Mono
            .fromCallable(() -> {
                PropertyHistory _his = historyStore.get(key.toTag());
                if (_his != null) {

                    PropertyHistory l1 = l1Cache.computeIfAbsent(
                        key, _ignore -> new PropertyHistory().setDirty(true));
                    if (l1.isDirty() && _his != l1) {
                        l1.merge(_his);
                    }
                    return mapper.apply(l1);

                }
                if (his != null) {
                    return mapper.apply(his);
                }
                return null;
            })
            .subscribeOn(readerScheduler);
    }

    @Override
    public Mono<Long> getLastPropertyTime(String thingType, String thingId, long baseTime) {
        return Mono.fromCallable(() -> {
                       long time = scanProperty(thingType,
                                                thingId,
                                                0L,
                                                baseTime,
                                                (init, arg, key, history) -> {
                                                    Property store = history.getProperty(arg);
                                                    if (store != null) {
                                                        return Math.max(init, store.time);
                                                    }
                                                    return init;

                                                });
                       return time == 0 ? null : time;
                   })
                   .subscribeOn(readerScheduler);
    }

    protected <T, ARG> T scanProperty(String thingType,
                                      String thingId,
                                      T init,
                                      ARG arg,
                                      Function4<T, ARG, Long, PropertyHistory, T> historyConsumer) {
        long thingTag = getThingIndex(thingType, thingId);

        int tagSize = tagStore.size();

        //左移32位表示物ID标记
        long fromTag = thingTag << 32;
        //加上标签总大小,表示可能的所有属性key范围
        long toTag = fromTag + tagSize + 1;
        //获取搜索的key范围
        Long fromKey = historyStore.higherKey(fromTag);
        //没有key,说明没有数据
        if (fromKey == null) {
            return init;
        }
        Long toKey = historyStore.lowerKey(toTag);

        //查找大于此标记的key,可能是同一个物的属性数据
        Cursor<Long, PropertyHistory> cursor = historyStore.cursor(fromKey, toKey, false);
        if (cursor == null) {
            return init;
        }

        //迭代游标来对比数据
        while (cursor.hasNext()) {
            long key = cursor.getKey();
            long tag = key >> 32;

            if (tag == thingTag) {
                PropertyHistory propertyStore = cursor.getValue();
                //与一级缓存进行合并
                PropertyHistory l1 = l1Cache.get(new StoreKey(thingTag, (int) (key - (key << 32))));
                if (l1 != null && l1.isDirty()) {
                    l1.merge(propertyStore);
                    propertyStore = l1;
                }
                init = historyConsumer.apply(init, arg, key, propertyStore);
            }

            cursor.next();
        }
        return init;
    }

    @Override
    public Mono<Long> getFirstPropertyTime(String thingType, String thingId) {
        return Mono
            .fromCallable(
                () -> scanProperty(
                    thingType,
                    thingId,
                    (Long) null,
                    null,
                    (init, arg, key, history) -> {
                        Property store = history.first;
                        if (store != null) {
                            if (init == null) {
                                return store.time;
                            }
                            return Math.min(init, store.time);
                        }
                        return init;

                    }))
            .subscribeOn(readerScheduler);
    }

    protected final int getIndex(String key) {
        return tagCache
            .computeIfAbsent(key, _key ->
                tagStore.computeIfAbsent(_key, k -> TAG_INC.incrementAndGet(this)));
    }

    @SneakyThrows
    protected ObjectOutput createOutput(ByteBuf buffer) {
        return Serializers.getDefault().createOutput(new ByteBufOutputStream(buffer));
    }

    @SneakyThrows
    protected ObjectInput createInput(ByteBuf buffer) {
        return Serializers.getDefault().createInput(new ByteBufInputStream(buffer, true));
    }

    @Nonnull
    @Override
    public final Mono<Void> updateProperty(@Nonnull String thingType, @Nonnull String thingId, @Nonnull ThingProperty property) {
        return updateProperty(thingType,
                              thingId,
                              property.getProperty(),
                              property.getTimestamp(),
                              property.getValue(),
                              property.getState());
    }

    protected long getThingIndex(String thingType, String thingId) {
        return getIndex(StringBuilderUtils.buildString(
            thingType, thingId,
            (a, b, sb) -> sb.append(a).append(':').append(b)));
    }

    protected long getPropertyStoreIndex(String thingType, String thingId, String property) {

        long thingTag = getThingIndex(thingType, thingId);

        int propertyTag = getIndex(property);

        //物ID对应的tag左移32位和属性tag相加,表示 一个物的某个属性.
        return (thingTag << 32) + propertyTag;
    }

    protected StoreKey getPropertyStoreKeyObj(String thingType, String thingId, String property) {
        long thingTag = getThingIndex(thingType, thingId);
        int propertyTag = getIndex(property);
        return new StoreKey(thingTag, propertyTag);
    }

    @Nonnull
    @Override
    public Mono<Void> updateProperty(@Nonnull String thingType,
                                     @Nonnull String thingId,
                                     @Nonnull String property,
                                     long timestamp,
                                     @Nonnull Object value,
                                     String state) {
        return Mono.fromRunnable(() -> updateProperty0(thingType, thingId, property, timestamp, value, state));
    }

    protected final void updateProperty0(String thingType,
                                         String thingId,
                                         String property,
                                         long timestamp,
                                         Object value,
                                         String state) {


        StoreKey key = getPropertyStoreKeyObj(thingType, thingId, property);

        PropertyHistory propertyStore = l1Cache.computeIfAbsent(key, k -> new PropertyHistory().setDirty(true));
        Property p = new Property();
        p.setTime(timestamp);
        p.setValue(tryIntern(value));
        p.setState(RecyclerUtils.intern(state));
        propertyStore.update(p);
    }

    protected final Mono<Void> updateProperty(String thingType,
                                              String thingId,
                                              String property,
                                              PropertyHistory propertyHistory) {
        StoreKey storeKey = getPropertyStoreKeyObj(thingType, thingId, property);

        PropertyHistory history = l1Cache
            .computeIfAbsent(storeKey, (ignore) -> new PropertyHistory().setDirty(true));

        //直接合并1级缓存
        history.merge(propertyHistory);

        return Mono.empty();

    }

    @Nonnull
    @Override
    public Mono<Void> updateEvent(@Nonnull String thingType,
                                  @Nonnull String thingId,
                                  @Nonnull String eventId,
                                  long timestamp,
                                  @Nonnull Object data) {
        return this.updateProperty(thingType, thingId, createEventProperty(eventId), timestamp, data, null);
    }

    @Nonnull
    @Override
    public Mono<Void> updateTag(@Nonnull String thingType,
                                @Nonnull String thingId,
                                @Nonnull String tagKey,
                                long timestamp,
                                @Nonnull Object tagValue) {
        return this.updateProperty(thingType, thingId, createTagProperty(tagKey), timestamp, tagValue, null);
    }

    @Nonnull
    @Override
    public Mono<Void> removeProperties(@Nonnull String thingType, @Nonnull String thingId) {


        return Mono
            .<Void>fromCallable(() -> this
                .scanProperty(
                    thingType,
                    thingId,
                    null,
                    null,
                    (init, arg, key, value) -> {
                        long thingTag = key >> 32;
                        int propertyTag = (int) (key - (key << 32));

                        historyStore.remove(key);
                        l1Cache.remove(new StoreKey(thingTag, propertyTag));
                        return null;
                    }))
            .subscribeOn(writerScheduler);

    }

    @Nonnull
    @Override
    public Mono<Void> removeEvent(@Nonnull String thingType,
                                  @Nonnull String thingId,
                                  @Nonnull String eventId) {
        return this.removeProperty(thingType, thingId, createEventProperty(eventId));
    }

    @Nonnull
    @Override
    public Mono<Void> removeTag(@Nonnull String thingType,
                                @Nonnull String thingId,
                                @Nonnull String tagKey) {
        return this.removeProperty(thingType, thingId, createTagProperty(tagKey));
    }

    @Nonnull
    @Override
    public Mono<Void> removeProperty(@Nonnull String thingType,
                                     @Nonnull String thingId,
                                     @Nonnull String property) {
        StoreKey key = getPropertyStoreKeyObj(thingType, thingId, property);

        l1Cache.remove(key);

        return Mono
            .fromRunnable(() -> historyStore.remove(key.toTag()))
            .subscribeOn(writerScheduler)
            .then();
    }

    @Override
    public Mono<ThingEvent> getLastEvent(String thingType,
                                         String thingId,
                                         String event,
                                         long baseTime) {
        String eventKey = createEventProperty(event);
        return this.getHistory(
            thingType,
            thingId,
            eventKey,
            prop -> {
                Property p = prop.getProperty(baseTime);
                if (p == null) {
                    return null;
                }
                return new PropertyThingEvent(event, p.time, p.value);
            });
    }

    protected String createEventProperty(String event) {
        return "e@" + event;
    }

    @AllArgsConstructor
    @Getter
    private static class PropertyThingEvent implements ThingEvent {
        private final String event;
        private final long timestamp;
        private final Object data;

    }

    @Override
    public Mono<ThingTag> getLastTag(String thingType,
                                     String thingId,
                                     String tag,
                                     long baseTime) {
        String key = createTagProperty(tag);
        return this.getHistory(
            thingType,
            thingId,
            key,
            prop -> {
                Property p = prop.getProperty(baseTime);
                if (p == null) {
                    return null;
                }
                return new PropertyThingTag(tag, p.time, p.value);
            });
    }

    protected String createTagProperty(String tag) {
        return "t@" + tag;
    }

    @AllArgsConstructor
    @Getter
    private static class PropertyThingTag implements ThingTag {
        private final String tag;
        private final long timestamp;
        private final Object value;

    }

    @AllArgsConstructor
    @EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
    protected static class StoreKey {
        protected final long thingTag;
        protected final int propertyTag;

        public long toTag() {
            return (thingTag << 32) + propertyTag;
        }
    }

    public static class PropertyHistory implements Externalizable {
        private static final byte
            STORED = 1, //已经持久化
            IDLE = 1 << 1,  //空闲的
            DIRTY = 1 << 2; //脏数据,等待和磁盘中的数据合并
        private static final Property NULL = new Property();

        static {
            NULL.setTime(Long.MIN_VALUE);
        }

        private Property first;
        private volatile Property last;

        //   private long minTime = -1;

        private transient volatile byte state;

        PropertyHistory merge(PropertyHistory another) {
            if (another == null) {
                return this;
            }
            synchronized (this) {
                if (this.first == null) {
                    this.first = another.first.copy();
                } else if (another.first != null && another.first.time < this.first.time) {
                    this.first = another.first.copy();
                }
                if (another.last != null) {
                    another.last.forEach(pro -> updateLastUnsafe(pro.copy()));
                }
            }
            return this;
        }

        @SuppressWarnings("all")
        void setStateUnsafe(boolean state, byte flag) {
            if (state) {
                this.state |= flag;
            } else {
                this.state &= ~flag;
            }
        }

        boolean isStored() {
            synchronized (this) {
                return (state & STORED) != 0;
            }
        }

        void setStored(boolean stored) {
            synchronized (this) {
                setStateUnsafe(stored, STORED);
            }
        }

        boolean isIdle() {
            synchronized (this) {
                return (state & IDLE) != 0;
            }
        }

        void setIdle(boolean idle) {
            synchronized (this) {
                setStateUnsafe(idle, IDLE);
            }
        }

        boolean isDirty() {
            synchronized (this) {
                return (state & DIRTY) != 0;
            }
        }

        PropertyHistory setDirty(boolean dirty) {
            synchronized (this) {
                setStateUnsafe(dirty, DIRTY);
            }
            return this;
        }


        public Property getProperty(long baseTime) {
            setIdle(false);
            if (last == null) {
                return null;
            }
            synchronized (this) {
                return last.getProperty(baseTime);
            }
        }

        public Long lastKey() {
            setIdle(false);
            if (last == null) {
                return null;
            }
            return last.time;
        }

        public List<ThingProperty> getProperties(String property, long from, long to) {
            if (last == null) {
                return Collections.emptyList();
            }
            if (DEFAULT_MAX_STORE_SIZE_EACH_KEY == 0) {
                return Collections.emptyList();
            }
            List<ThingProperty> properties = new ArrayList<>(Math.min(32, DEFAULT_MAX_STORE_SIZE_EACH_KEY));
            synchronized (this) {
                setStateUnsafe(false, IDLE);
                Property prop = last;
                while (prop != null) {
                    if (prop.time >= from && prop.time < to) {
                        properties.add(prop.toProperty(property));
                        prop = prop.older;
                    } else {
                        break;
                    }
                }
            }
            return properties;
        }


        @SuppressWarnings("all")
        public void updateLastUnsafe(Property ref) {
            if (last == null) {
                last = ref;
            } else {
                last = last.update(ref, 1);
            }
        }

        @SuppressWarnings("all")
        public void updateUnsafe(Property ref) {
            //更新首次时间
            if (first == null) {
                first = ref;
            } else if (first.time >= ref.time) {
                first.time = ref.time;
                first.state = ref.state;
                first.value = ref.value;
            }
            updateLastUnsafe(ref);
            setStateUnsafe(false, IDLE);
        }

        Collection<Property> values(Collection<Property> properties) {
            for (Property prop = last; prop != null; prop = prop.older) {
                properties.add(prop);
            }
            return properties;
        }

        Collection<Property> values() {
            return values(new ArrayList<>(DEFAULT_MAX_STORE_SIZE_EACH_KEY));
        }

        //更新
        public void update(Property ref) {
            synchronized (this) {
                updateUnsafe(ref);
                setStateUnsafe(false, STORED);
            }
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            Collection<Property> properties = this.values();
            int size = Math.min(properties.size(), DEFAULT_MAX_STORE_SIZE_EACH_KEY);
            out.writeShort(size);
            for (Property ref : properties) {
                if (size-- == 0) {
                    break;
                }
                ref.writeExternal(out);
            }
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    NULL.writeExternal(out);
                }
            }
            out.writeBoolean(first != null);
            if (first != null) {
                first.writeExternal(out);
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            this.setStored(true);
            int len = in.readShort();
            synchronized (this) {
                for (int i = 0; i < len; i++) {
                    Property property = new Property();
                    property.readExternal(in);
                    if (property.time == NULL.time) {
                        continue;
                    }
                    this.updateLastUnsafe(property);
                }
                if (in.readBoolean()) {
                    this.first = new Property();
                    first.readExternal(in);
                }
            }
        }

        public int memory() {
            if (last != null) {
                return last.memory();
            }
            return 16;
        }
    }

    public static <T> T tryIntern(T val) {
        //针对数字类型进行常量化
        if (val instanceof Number) {
            if (val instanceof BigDecimal) {
                BigDecimal decimal = ((BigDecimal) val);
                //整数
                if (decimal.scale() == 0) {
                    int intVal = decimal.intValue();
                    if (intVal > Short.MIN_VALUE && intVal < 65535) {
                        return RecyclerUtils.intern(val);
                    }
                }
                //2位小数
                else if (decimal.scale() <= 2) {
                    double intVal = decimal.doubleValue() * Math.pow(10, decimal.scale());
                    if (intVal > Short.MIN_VALUE && intVal < 65535) {
                        return RecyclerUtils.intern(val);
                    }
                }
            } else if (val instanceof BigInteger) {
                long value = ((BigInteger) val).longValue();
                if (value > Short.MIN_VALUE && value < 65535) {
                    return RecyclerUtils.intern(val);
                }
            } else if (NumberUtils.isIntNumber(((Number) val))) {
                int v = ((Number) val).intValue();
                if (v > Short.MIN_VALUE && v < 65535) {
                    return RecyclerUtils.intern(val);
                }
            } else {
                double v = ((Number) val).doubleValue();
                //缓存2位小数
                if (v > Byte.MIN_VALUE &&
                    v < Byte.MAX_VALUE &&
                    v * 1000 == (int) (v * 1000)) {
                    return RecyclerUtils.intern(val);
                }
            }
        }

        //字符串长度小于64常量化
        if (val instanceof String) {
            if (((String) val).length() < 64) {
                return RecyclerUtils.intern(val);
            }
        }
        return val;
    }

    @Getter
    @Setter
    public static class Property implements Externalizable {
        private long time;
        private String state;
        private Object value;
        private transient Property older;

        int olderSize() {
            int i = 0;
            Property _o = older;
            while (_o != null) {
                i++;
                _o = _o.older;
            }
            return i;
        }

        public Property copy() {
            Property property = new Property();
            property.time = this.time;
            property.state = this.state;
            property.value = this.value;
            return property;
        }

        protected void forEach(Consumer<Property> consumer) {
            for (Property that = this;
                 that != null;
                 that = that.older) {
                consumer.accept(that);
            }
        }

        private Property getProperty(long baseTime) {
            if (time <= baseTime) {
                return this;
            }
            if (older != null) {
                return older.getProperty(baseTime);
            }
            return null;
        }

        private void computeCapacity(int deep) {
            int itr = DEFAULT_MAX_STORE_SIZE_EACH_KEY - deep;
            if (itr <= 0) {
                this.older = null;
                return;
            }
            Property node = this.older;
            while (node != null && --itr > 0) {
                node = node.older;
            }
            if (node != null) {
                node.older = null;
            }
        }

        public Property update(Property property) {
            return update(property, 1);
        }

        private Property update(Property property, int deep) {
            if (this == property) {
                return this;
            }
            //新的数据,更新节点
            if (property.time > this.time) {
                property.older = this;
                property.computeCapacity(deep);
                return property;
            }
            //旧的数据?
            if (property.time < this.time) {
                if (older != null) {
                    older = older.update(property, deep + 1);
                } else {
                    property.older = null;
                    older = property;
                }
                computeCapacity(deep);
                return this;
            }
            //相同时间,直接更新值
            this.state = property.state;
            this.value = property.value;
            return this;
        }

        public ThingProperty toProperty(String property) {
            return SimplePropertyValue.of(property, value, time, state);
        }

        @SneakyThrows
        @SuppressWarnings("all")
        public ThingProperty toPropertyNow(String property) {
            return SimplePropertyValue.of(property, value, time, state);
        }

        public int memory() {
            // 对象固定大小 this (32) + time (8)
            int i = 40;

            for (Property property = this;
                 property != null;
                 property = property.older) {
                Object value = property.value;
                //数字,固定8
                if (value instanceof Number) {
                    i += 8;
                } else if (value instanceof String) {
                    i += ((String) value).length() * 2;
                } else {
                    i += 64;
                }
                if (property.state != null) {
                    i += property.state.length() * 2;
                }
            }
            return i;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeLong(time);
            SerializeUtils.writeObject(state, out);
            SerializeUtils.writeObject(value, out);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            time = in.readLong();
            state = (String) tryIntern(SerializeUtils.readObject(in));
            this.value = tryIntern(SerializeUtils.readObject(in));
        }

        @Override
        public String toString() {
            return value + "(" + time + ")";
        }
    }

    private class HistoryType extends BasicDataType<PropertyHistory> {

        @Override
        public int compare(PropertyHistory a, PropertyHistory b) {
            Long aLastKey = a.lastKey();
            Long bLastKey = b.lastKey();

            if (aLastKey == null && bLastKey == null) {
                return 0;
            }
            if (aLastKey == null) {
                return -1;
            }
            if (bLastKey == null) {
                return 1;
            }
            return Long.compare(aLastKey, bLastKey);
        }

        @Override
        public int getMemory(PropertyHistory obj) {
            return obj.memory();
        }

        @Override
        @SneakyThrows
        public void write(WriteBuffer buff, PropertyHistory data) {
            ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            try (ObjectOutput output = createOutput(buffer)) {

                data.writeExternal(output);
                output.flush();

                buff.put(buffer.nioBuffer());
            } catch (Throwable err) {
                log.warn("write thing data error", err);
            } finally {
                ReferenceCountUtil.safeRelease(buffer);
            }

        }

        @Override
        @SneakyThrows
        public void write(WriteBuffer buff, Object obj, int len) {
            ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            try (ObjectOutput output = createOutput(buffer)) {

                for (int i = 0; i < len; i++) {
                    PropertyHistory history = ((PropertyHistory) Array.get(obj, i));
                    if (history != null) {
                        history.writeExternal(output);
                    }
                }
                output.flush();

                buff.put(buffer.nioBuffer());

            } catch (Throwable err) {
                log.warn("write thing data error", err);
            } finally {
                ReferenceCountUtil.safeRelease(buffer);
            }
        }

        @Override
        @SneakyThrows
        public void read(ByteBuffer buff, Object obj, int len) {
            try (ObjectInput input = createInput(Unpooled.wrappedBuffer(buff))) {
                for (int i = 0; i < len; i++) {
                    PropertyHistory data = new PropertyHistory();
                    data.readExternal(input);
                    Array.set(obj, i, data);
                }
            } catch (Throwable err) {
                log.warn("read thing data error", err);
            }
        }

        @Override
        public PropertyHistory[] createStorage(int size) {
            return new PropertyHistory[size];
        }

        @Override
        @SneakyThrows
        public PropertyHistory read(ByteBuffer buff) {
            PropertyHistory data = new PropertyHistory();
            try (ObjectInput input = createInput(Unpooled.wrappedBuffer(buff))) {
                data.readExternal(input);
            } catch (Throwable err) {
                log.warn("read thing data error", err);
            }
            return data;
        }

    }
}
