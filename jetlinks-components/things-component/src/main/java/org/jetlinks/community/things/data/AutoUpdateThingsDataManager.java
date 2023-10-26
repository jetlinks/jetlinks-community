package org.jetlinks.community.things.data;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.*;
import org.jetlinks.community.things.ThingConstants;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.message.property.PropertyMessage;
import org.jetlinks.core.things.ThingId;
import org.jetlinks.core.things.ThingProperty;
import org.jetlinks.core.things.ThingType;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按需更新的物数据管理器
 * <p>
 * 如果是首次获取将从事件总线订阅数据并更新.
 * 如果超过10分钟没有获取过数据,则将取消订阅.
 * <p>
 * 缓存数据会持久化到指定的文件中.
 * <p>
 * 当获取的本地缓存不存在,并且集群的其他节点也没有获取过,可能将获取到旧数据.
 *
 * @author zhouhao
 * @since 2.0
 */
public class AutoUpdateThingsDataManager extends LocalFileThingsDataManager {

    private final Map<ThingId, Updater> updaters = Caffeine
        .newBuilder()
        //10分钟没有任何读取则dispose取消订阅
        .expireAfterAccess(Duration.ofMinutes(10))
        .<ThingId, Updater>removalListener((key, value, cause) -> {
            if (cause == RemovalCause.EXPIRED) {
                if (value != null) {
                    value.dispose();
                }
            }
        })
        .build()
        .asMap();

    private final EventBus eventBus;

    public AutoUpdateThingsDataManager(String fileName, EventBus eventBus) {
        super(fileName);
        this.eventBus = eventBus;
    }

    @Override
    public Mono<List<ThingProperty>> getProperties(String thingType, String thingId, String property, long from, long to) {
        Updater updater = getUpdater(thingType, thingId);
        updater.tryLoad(property);
        Mono<Void> loader = updater.loader;
        if (updater.loading && loader != null) {
            return loader
                .then(Mono.defer(() -> super.getProperties(thingType, thingId, property, from, to)));
        }
        return super.getProperties(thingType, thingId, property, from, to);
    }

    @Override
    public Mono<ThingProperty> getLastProperty(String thingType,
                                               String thingId,
                                               String property,
                                               long baseTime) {
        Updater updater = getUpdater(thingType, thingId);
        updater.tryLoad(property);
        Mono<Void> loader = updater.loader;

        if (updater.loading && loader != null) {
            return loader
                .then(Mono.defer(() -> super.getLastProperty(thingType, thingId, property, baseTime)));
        }
        return super.getLastProperty(thingType, thingId, property, baseTime);
    }

    @Override
    public Mono<ThingProperty> getFirstProperty(String thingType, String thingId, String property) {
        Updater updater = getUpdater(thingType, thingId);
        updater.tryLoad(property);

        Mono<Void> loader = updater.loader;

        if (updater.loading && loader != null) {
            return loader
                .then(Mono.defer(() -> super.getFirstProperty(thingType, thingId, property)));
        }
        return super.getFirstProperty(thingType, thingId, property);
    }

    private Updater getUpdater(String thingType, String thingId) {
        ThingId key = ThingId.of(thingType, thingId);
        return updaters
            .computeIfAbsent(key, this::createUpdater);
    }

    private Mono<Void> loadData(String thingType, String thingId, String property) {
        //fixme 不加载不存在的数据,如果查库可能会导致瞬间压力过高
        return Mono.empty();
    }

    protected Updater createUpdater(ThingId id) {
        return new Updater(id.getType(), id.getId());
    }

    @SneakyThrows
    private ByteBuf encodeHistory(PropertyHistory history) {
        ByteBuf buf = Unpooled.buffer();
        try (ObjectOutput out = createOutput(buf)) {
            history.writeExternal(out);
        }
        return buf;
    }

    @SneakyThrows
    private PropertyHistory decodeHistory(ByteBuf buf) {
        PropertyHistory history = new PropertyHistory();
        try (ObjectInput input = createInput(buf)) {
            history.readExternal(input);
        }
        return history;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        updaters.values().forEach(Disposable::dispose);
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class ThingHistoryRequest {
        private String thingType;
        private String thingId;
        private String property;
    }

    protected class Updater implements Disposable {
        private final String thingType;
        private final String thingId;
        private final Disposable disposable;

        private final Set<String> include = ConcurrentHashMap.newKeySet();

        private boolean loading;
        private Mono<Void> loader;

        public Updater(String thingType, String thingId) {
            this.thingType = thingType;
            this.thingId = thingId;
            //订阅整个集群的消息来更新本地的缓存数据
            disposable = eventBus
                .subscribe(
                    Subscription.builder()
                                .subscriberId("thing-data-property-updater")
                                .topics(ThingConstants.Topics.properties(ThingType.of(thingType), thingId))
                                .local()
                                .broker()
                                .priority(Integer.MIN_VALUE)
                                .build(),
                    ThingMessage.class
                )
                .doOnNext(this::doUpdate)
                .subscribe();
        }

        private void doUpdate(ThingMessage thingMessage) {
            if (!(thingMessage instanceof PropertyMessage)) {
                return;
            }
            PropertyMessage message = (PropertyMessage) thingMessage;
            try {
                Map<String, Object> properties = message.getProperties();
                if (properties == null) {
                    return;
                }
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    String property = entry.getKey();

                    if (include.contains(property)) {
                        updateProperty0(thingType,
                                        thingId,
                                        property,
                                        message
                                            .getPropertySourceTime(property)
                                            .orElse(message.getTimestamp()),
                                        entry.getValue(),
                                        message.getPropertyState(property).orElse(null)
                        );
                    }
                }
            } catch (Throwable ignore) {
                //ignore
            }
        }

        private void tryLoad(String property) {
            if (include.add(property)) {
                this.loading = true;
                this.loader = loadData(thingType, thingId, property)
                    .doAfterTerminate(() -> {
                        loading = false;
                        loader = null;
                    })
                    .cache();
            }
        }

        @Override
        public void dispose() {
            disposable.dispose();
            include.clear();
        }

        @Override
        public boolean isDisposed() {
            return disposable.isDisposed();
        }
    }

}
