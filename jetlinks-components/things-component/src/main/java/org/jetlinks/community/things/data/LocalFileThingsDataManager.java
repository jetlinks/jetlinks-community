package org.jetlinks.community.things.data;

import io.netty.buffer.*;
import io.netty.util.ReferenceCountUtil;
import lombok.*;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jetlinks.community.codec.Serializers;
import org.jetlinks.core.things.ThingEvent;
import org.jetlinks.core.things.ThingProperty;
import org.jetlinks.core.things.ThingsDataManager;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.supports.utils.MVStoreUtils;
import reactor.core.publisher.Mono;
import reactor.function.Function4;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;

public class LocalFileThingsDataManager implements ThingsDataManager, ThingsDataWriter {

    //单个属性最大缓存数量 java -Dthings.data.store.max-size=8
    static int DEFAULT_MAX_STORE_SIZE_EACH_KEY = Integer
        .parseInt(
            System.getProperty("jetlinks.things.data.store.max-size", "8")
        );

    protected final MVStore mvStore;

    //记录key的标签缓存,此方式决定了支持的最大(物实例数量+属性数量)为2^32(42亿)
    private final Map<String, Integer> tagCache = new ConcurrentHashMap<>();
    private final MVMap<String, Integer> tagStore;

    //  历史数据缓存
    //  fixme 内存占用可能过多, 根据当前jvm内存来决定使用CaffeineCache还是ConcurrentHashMap
    // key为什么不直接使用Long?因为tag的生成规则会导致hash冲突严重.(同一个物实例的所有属性tag hash值一样)
    private final Map<StoreKey, PropertyHistory> historyCache = new ConcurrentHashMap<>();
    private final MVMap<Long, PropertyHistory> historyStore;

    @SuppressWarnings("all")
    private static MVStore load(String fileName) {
        return MVStoreUtils
            .open(new File(fileName),
                  "things-data-manager",
                  c -> {
                      return c.keysPerPage(1024)
                              .cacheSize(64);
                  });
    }

    public LocalFileThingsDataManager(String fileName) {
        this(load(fileName));
    }

    public LocalFileThingsDataManager(MVStore store) {
        this.mvStore = store;
        this.tagStore = mvStore.openMap("tags");
        this.historyStore = mvStore
            .openMap("store", new MVMap
                .Builder<Long, PropertyHistory>()
                .valueType(new HistoryType()));
    }

    public void shutdown() {
        for (Map.Entry<StoreKey, PropertyHistory> entry : historyCache.entrySet()) {
            if (!entry.getValue().stored) {
                entry.getValue().stored = true;
                historyStore.put(entry.getKey().toTag(), entry.getValue());
            }
        }
        for (Map.Entry<Long, PropertyHistory> entry : historyStore.entrySet()) {
            if (!entry.getValue().stored) {
                historyStore.put(entry.getKey(), entry.getValue());
            }
        }
        mvStore.compactFile(60_000);
        mvStore.close(60_000);
    }

    @Override
    public Mono<ThingProperty> getLastProperty(String thingType,
                                               String thingId,
                                               String property,
                                               long baseTime) {
        PropertyHistory propertyStore = getHistory(thingType, thingId, property);
        if (propertyStore == null) {
            return lastPropertyNotFound(thingType, thingId, property, baseTime);
        }
        Property pro = propertyStore.getProperty(baseTime);
        if (pro == null) {
            return lastPropertyNotFound(thingType, thingId, property, baseTime);
        }
        return pro.toProperty(property);
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
        PropertyHistory propertyStore = getHistory(thingType, thingId, property);
        if (propertyStore == null) {
            return firstPropertyNotFound(thingType, thingId, property);
        }
        Property pro = propertyStore.first;
        if (pro == null) {
            return firstPropertyNotFound(thingType, thingId, property);
        }
        return pro.toProperty(property);
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
        PropertyHistory propertyStore = getHistory(thingType, thingId, property);
        if (propertyStore == null) {
            return Mono.empty();
        }
        return Mono.just(propertyStore.getProperties(property, from, to));
    }

    protected PropertyHistory getHistory(String thingType,
                                         String thingId,
                                         String property) {
        StoreKey key = getPropertyStoreKeyObj(thingType, thingId, property);
        PropertyHistory his = historyCache.get(key);
        if (his != null) {
            return his;
        }
        his = historyStore.get(key.toTag());
        if (his != null) {
            historyCache.putIfAbsent(key, his);
            return his;
        }
        return null;
    }

    @Override
    public Mono<Long> getLastPropertyTime(String thingType, String thingId, long baseTime) {
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
        return time == 0 ? Mono.empty() : Mono.just(time);
    }

    protected <T, ARG> T scanProperty(String thingType,
                                      String thingId,
                                      T init,
                                      ARG arg,
                                      Function4<T, ARG, Long, PropertyHistory, T> historyConsumer) {
        long thingTag = getThingTag(thingType, thingId);

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
                init = historyConsumer.apply(init, arg, key, propertyStore);
            }

            cursor.next();
        }
        return init;
    }

    @Override
    public Mono<Long> getFirstPropertyTime(String thingType, String thingId) {
        Long time = scanProperty(thingType,
                                 thingId,
                                 null,
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

                                 });
        return time == null ? Mono.empty() : Mono.just(time);
    }

    protected final int getTag(String key) {
        return tagCache
            .computeIfAbsent(key, _key ->
                tagStore.computeIfAbsent(_key, k -> tagStore.size() + 1));
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

    protected long getThingTag(String thingType, String thingId) {
        return getTag(StringBuilderUtils.buildString(
            thingType, thingId,
            (a, b, sb) -> sb.append(a).append(':').append(b)));
    }

    protected long getPropertyStoreTag(String thingType, String thingId, String property) {

        long thingTag = getThingTag(thingType, thingId);

        int propertyTag = getTag(property);

        //物ID对应的tag左移32位和属性tag相加,表示 一个物的某个属性.
        return (thingTag << 32) + propertyTag;
    }

    protected StoreKey getPropertyStoreKeyObj(String thingType, String thingId, String property) {
        long thingTag = getThingTag(thingType, thingId);
        int propertyTag = getTag(property);
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
        updateProperty0(thingType, thingId, property, timestamp, value, state);
        return Mono.empty();
    }

    protected final void updateProperty0(String thingType,
                                         String thingId,
                                         String property,
                                         long timestamp,
                                         Object value,
                                         String state) {

        StoreKey key = getPropertyStoreKeyObj(thingType, thingId, property);

        PropertyHistory propertyStore = historyCache
            .computeIfAbsent(key, k -> historyStore.computeIfAbsent(k.toTag(), k1 -> new PropertyHistory()));

        Property p = new Property();
        p.setTime(timestamp);
        p.setValue(value);
        p.setState(state);
        propertyStore.update(p);
        propertyStore.tryStore(key.toTag(), historyStore::put);
    }

    protected final void updateProperty(String thingType,
                                        String thingId,
                                        String property,
                                        PropertyHistory propertyHistory) {
        long key = getPropertyStoreTag(thingType, thingId, property);
        PropertyHistory propertyStore = historyStore.computeIfAbsent(key, (ignore) -> new PropertyHistory());
        if (propertyHistory.first != null) {
            propertyStore.update(propertyHistory.first);
        }
        for (Property ref : propertyHistory.refs.values()) {
            propertyStore.update(ref);
        }
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
    public Mono<Void> removeProperties(@Nonnull String thingType, @Nonnull String thingId) {

        scanProperty(thingType, thingId, null, null, (init, arg, key, value) -> {
            long thingTag = key >> 32;
            int propertyTag = (int) (key - (key << 32));

            historyStore.remove(key);
            historyCache.remove(new StoreKey(thingTag, propertyTag));
            return null;
        });

        return Mono.empty();

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
    public Mono<Void> removeProperty(@Nonnull String thingType,
                                     @Nonnull String thingId,
                                     @Nonnull String property) {
        StoreKey key = getPropertyStoreKeyObj(thingType, thingId, property);

        historyCache.remove(key);

        historyStore.remove(key.toTag());

        return Mono.empty();
    }

    @Override
    public Mono<org.jetlinks.core.things.ThingEvent> getLastEvent(String thingType,
                                                                  String thingId,
                                                                  String event,
                                                                  long baseTime) {
        String eventKey = createEventProperty(event);
        PropertyHistory propertyStore = getHistory(thingType, thingId, eventKey);
        if (propertyStore == null) {
            return Mono.empty();
        }
        Property pro = propertyStore.getProperty(baseTime);
        if (pro == null) {
            return Mono.empty();
        }
        return pro
            .toProperty(eventKey)
            .map(PropertyThingEvent::new);
    }

    protected String createEventProperty(String event) {
        return "e@" + event;
    }

    @AllArgsConstructor
    private static class PropertyThingEvent implements ThingEvent {
        private final ThingProperty property;

        @Override
        public String getEvent() {
            return property
                .getProperty()
                .substring(2);
        }

        @Override
        public long getTimestamp() {
            return property.getTimestamp();
        }

        @Override
        public Object getData() {
            return property.getValue();
        }
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

        private Property first;

        private final NavigableMap<Long, Property> refs = new ConcurrentSkipListMap<>();

        private long minTime = -1;

        private long elapsedTime;

        private boolean stored;

        public Property getProperty(long baseTime) {
            Map.Entry<Long, Property> ref = refs.floorEntry(baseTime);
            if (ref != null) {
                return ref.getValue();
            }
            return null;
        }

        public List<ThingProperty> getProperties(String property, long from, long to) {
            if (refs.isEmpty()) {
                return Collections.emptyList();
            }
            if (DEFAULT_MAX_STORE_SIZE_EACH_KEY == 0) {
                return Collections.emptyList();
            }

            List<ThingProperty> properties = new ArrayList<>(Math.min(32, DEFAULT_MAX_STORE_SIZE_EACH_KEY));
            refs.subMap(from, true, to, false)
                .forEach((ts, ref) -> {
                    ThingProperty prop = ref.toPropertyNow(property);
                    if (prop != null) {
                        properties.add(prop);
                    }
                });
            return properties;
        }

        public void tryStore(long key, BiConsumer<Long, PropertyHistory> store) {
            long now = System.currentTimeMillis();
            long elapsed = elapsedTime;
            elapsedTime = now;
            if (now - elapsed >= 5_000) {
                stored = true;
                store.accept(key, this);
            } else {
                stored = false;
            }
        }

        //更新
        public void update(Property ref) {

            //更新首次时间
            if (first == null || first.time >= ref.time) {
                first = ref;
            }

            if (minTime > 0) {
                //时间回退?
                if (ref.time < minTime) {
                    return;
                }
            }

            refs.put(ref.time, ref);
            if (refs.size() > DEFAULT_MAX_STORE_SIZE_EACH_KEY) {
                refs.remove(refs.firstKey());
            }
            minTime = refs.firstKey();

        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeShort(refs.size());
            for (Property ref : refs.values()) {
                ref.writeExternal(out);
            }
            out.writeBoolean(first != null);
            if (first != null) {
                first.writeExternal(out);
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            this.stored = true;
            int len = in.readShort();
            for (int i = 0; i < len; i++) {
                Property property = new Property();
                property.readExternal(in);
                refs.put(property.time, property);
            }
            if (in.readBoolean()) {
                first = new Property();
                first.readExternal(in);
            }
        }

        public int memory() {
            int i = 0;
            if (first != null) {
                i += first.memory();
            }
            for (Property ref : refs.values()) {
                if (ref != null) {
                    i += ref.memory();
                }
            }
            return i;
        }
    }

    @Getter
    @Setter
    public static class Property implements Externalizable {
        private long time;
        private String state;
        private Object value;

        private volatile Mono<ThingProperty> _temp;

        public Mono<ThingProperty> toProperty(String property) {
            if (_temp == null) {
                _temp = Mono.just(ThingProperty.of(property, value, time, state));
            }
            return _temp;
        }

        @SneakyThrows
        @SuppressWarnings("all")
        public ThingProperty toPropertyNow(String property) {
            if (_temp == null) {
                _temp = Mono.just(ThingProperty.of(property, value, time, state));
            }
            if (_temp instanceof Callable) {
                return ((Callable<ThingProperty>) _temp).call();
            }
            return _temp.toFuture().getNow(null);
        }

        public int memory() {
            int i = 8;
            if (state != null) {
                i += state.length() * 2;
            }
            if (value instanceof Number) {
                i += 8;
            } else {
                i += 64;
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
            state = (String) SerializeUtils.readObject(in);
            value = SerializeUtils.readObject(in);
        }
    }

    private class HistoryType extends BasicDataType<PropertyHistory> {

        @Override
        public int compare(PropertyHistory a, PropertyHistory b) {
            Long aLastKey = a.refs.lastKey();
            Long bLastKey = b.refs.lastKey();

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
                    ((PropertyHistory) Array.get(obj, i)).writeExternal(output);
                }
                output.flush();

                buff.put(buffer.nioBuffer());

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
            }
            return data;
        }

    }
}
