package org.jetlinks.community.things.data;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.buffer.*;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jetlinks.core.things.ThingProperty;
import org.jetlinks.core.things.ThingsDataManager;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.community.codec.Serializers;
import reactor.core.publisher.Mono;
import reactor.function.Function3;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class LocalFileThingsDataManager implements ThingsDataManager, ThingsDataWriter {

    //单个属性最大缓存数量 java -Dthing.data.store.max-size=4
    private final static int DEFAULT_MAX_STORE_SIZE_EACH_KEY = Integer
        .parseInt(
            System.getProperty("thing.data.store.max-size", "4")
        );

    protected final MVStore mvStore;

    private final Map<String, Integer> tagCache = new ConcurrentHashMap<>();
    private final Map<Long, PropertyHistory> historyCache =
        Caffeine
            .newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .<Long, PropertyHistory>build()
            .asMap();

    private final MVMap<String, Integer> tagStore;

    private final MVMap<Long, PropertyHistory> history;

    private static MVStore open(String fileName) {
        return new MVStore.Builder()
            .fileName(fileName)
            .autoCommitBufferSize(64 * 1024)
            .compress()
            .keysPerPage(1024)
            .cacheSize(64)
            .open();
    }

    @SuppressWarnings("all")
    private static MVStore load(String fileName) {
        File file = new File(fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            return open(fileName);
        } catch (Throwable err) {
            if (file.exists()) {
                file.renameTo(new File(fileName + "_load_err_" + System.currentTimeMillis()));
                file.delete();
                return open(fileName);
            } else {
                throw err;
            }
        }
    }

    public LocalFileThingsDataManager(String fileName) {
        this(load(fileName));
    }

    public LocalFileThingsDataManager(MVStore store) {
        this.mvStore = store;
        this.tagStore = mvStore.openMap("tags");
        this.history = mvStore
            .openMap("store", new MVMap
                .Builder<Long, PropertyHistory>()
                .valueType(new HistoryType()));
    }

    public void shutdown() {
        for (Map.Entry<Long, PropertyHistory> entry : historyCache.entrySet()) {
            if (!entry.getValue().stored) {
                entry.getValue().stored = true;
                history.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<Long, PropertyHistory> entry : history.entrySet()) {
            if (!entry.getValue().stored) {
                history.put(entry.getKey(), entry.getValue());
            }
        }
        mvStore.compactMoveChunks();
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
        long key = getPropertyStoreKey(thingType, thingId, property);
        PropertyHistory his = historyCache.get(key);
        if (his != null) {
            return his;
        }
        his = history.get(key);
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
                                 (init, arg, history) -> {
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
                                      Function3<T, ARG, PropertyHistory, T> historyConsumer) {
        long thingTag = getThingTag(thingType, thingId);

        int tagSize = tagStore.size();

        //左移32位表示物ID标记
        long fromTag = thingTag << 32;
        //加上标签总大小,表示可能的所有属性key范围
        long toTag = fromTag + tagSize;
        //获取搜索的key范围
        Long fromKey = history.higherKey(fromTag);
        //没有key,说明没有数据
        if (fromKey == null) {
            return init;
        }
        Long toKey = history.lowerKey(toTag);

        //查找大于此标记的key,可能是同一个物的属性数据
        Cursor<Long, PropertyHistory> cursor = history.cursor(fromKey, toKey, false);
        if (cursor == null) {
            return init;
        }
        final int maxLoop = tagSize / 2;
        int loop = maxLoop;

        //迭代游标来对比数据
        while (cursor.hasNext() && loop > 0) {
            long _tag = cursor.getKey() >> 32;
            if (_tag != thingTag) {
                loop--;
                cursor.next();
                continue;
            }
            loop = maxLoop;
            PropertyHistory propertyStore = cursor.getValue();
            init = historyConsumer.apply(init, arg, propertyStore);
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
                                 (init, arg, history) -> {
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

    protected long getPropertyStoreKey(String thingType, String thingId, String property) {

        long thingTag = getThingTag(thingType, thingId);

        int propertyTag = getTag(property);

        //物ID对应的tag左移32位和属性tag相加,表示 一个物的某个属性.
        return (thingTag << 32) + propertyTag;
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

        long key = getPropertyStoreKey(thingType, thingId, property);

        PropertyHistory propertyStore = historyCache
            .computeIfAbsent(key, k -> history.computeIfAbsent(k, k1 -> new PropertyHistory()));

        Property p = new Property();
        p.setTime(timestamp);
        p.setValue(value);
        p.setState(state);
        propertyStore.update(p);
        propertyStore.tryStore(key, history::put);
    }

    protected final void updateProperty(String thingType,
                                        String thingId,
                                        String property,
                                        PropertyHistory propertyHistory) {
        long key = getPropertyStoreKey(thingType, thingId, property);
        PropertyHistory propertyStore = history.computeIfAbsent(key, (ignore) -> new PropertyHistory());
        if (propertyHistory.first != null) {
            propertyStore.update(propertyHistory.first);
        }
        if (propertyHistory.refs != null) {
            for (Property ref : propertyHistory.refs) {
                propertyStore.update(ref);
            }
        }
    }

    public static class PropertyHistory implements Externalizable {
        private Property first;

        private Property[] refs;
        private long minTime = -1;

        private long elapsedTime;

        private boolean stored;

        public Property getProperty(long baseTime) {
            if (refs == null) {
                return null;
            }
            for (Property ref : refs) {
                if (ref != null && ref.time <= baseTime) {
                    return ref;
                }
            }
            return null;
        }

        public List<ThingProperty> getProperties(String property, long from, long to) {
            if (refs == null) {
                return Collections.emptyList();
            }
            if (DEFAULT_MAX_STORE_SIZE_EACH_KEY == 0) {
                return Collections.emptyList();
            }
            List<ThingProperty> properties = new ArrayList<>(Math.min(32, DEFAULT_MAX_STORE_SIZE_EACH_KEY));
            for (Property ref : refs) {
                if (ref != null && ref.time >= from && ref.time < to) {
                    ThingProperty prop = ref.toPropertyNow(property);
                    if (prop != null) {
                        properties.add(prop);
                    }
                }
            }
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

        //更新,并返回距离上传更新的时间差
        public void update(Property ref) {

            //init
            if (refs == null) {
                refs = new Property[0];
            }
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

            boolean newEl = false;
            if (refs.length < DEFAULT_MAX_STORE_SIZE_EACH_KEY) {
                refs = Arrays.copyOf(refs, refs.length + 1);
                newEl = true;
            }

            Property last = refs[0];
            //fast
            if (last == null || ref.time >= last.time || newEl) {
                refs[refs.length - 1] = ref;
            }
            //slow
            else {
                for (int i = 1; i < refs.length; i++) {
                    last = refs[i];
                    if (ref.time == last.time) {
                        refs[i] = ref;
                    } else if (ref.time > last.time) {
                        System.arraycopy(refs, i, refs, i + 1, refs.length - i - 1);
                        refs[i] = ref;
                        break;
                    }
                }
            }

            Arrays.sort(refs, Comparator.comparingLong(r -> r == null ? 0 : -r.time));
            minTime = refs[refs.length - 1].time;

            return;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeShort(refs.length);
            for (Property ref : refs) {
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

            refs = new Property[len];
            for (int i = 0; i < len; i++) {
                refs[i] = new Property();
                refs[i].readExternal(in);
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
            if (refs != null) {
                for (Property ref : refs) {
                    if (ref != null) {
                        i += ref.memory();
                    }
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
            if (a.refs == null && b.refs == null) {
                return 0;
            }
            if (a.refs == null) {
                return -1;
            }
            if (b.refs == null) {
                return 1;
            }
            return Long.compare(a.refs[0].time, b.refs[0].time);
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
                buff.put(buffer.nioBuffer());
                output.flush();
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
