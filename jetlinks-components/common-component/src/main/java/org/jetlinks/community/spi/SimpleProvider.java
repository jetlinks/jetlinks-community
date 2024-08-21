package org.jetlinks.community.spi;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.core.CastUtil;
import org.hswebframework.web.exception.BusinessException;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SimpleProvider<T> implements Provider<T> {

    private final Map<String, T> providers = new ConcurrentHashMap<>();

    private final List<Hook<T>> hooks = new CopyOnWriteArrayList<>();

    private final String name;


    public Disposable register(String id, T provider) {
        providers.put(id, provider);
        executeHook(provider, Hook::onRegister);
        return () -> unregister(id, provider);
    }

    public List<T> getAll() {
        return new ArrayList<>(providers.values());
    }

    public T registerIfAbsent(String id, T provider) {
        T old = providers.putIfAbsent(id, provider);
        if (old == null || old != provider) {
            executeHook(provider, Hook::onRegister);
        }
        return old;
    }

    @Override
    public T registerIfAbsent(String id, Function<String, T> providerBuilder) {
        Object[] absent = new Object[1];
        T old = providers
                .computeIfAbsent(id, _id -> {
                    T provider = providerBuilder.apply(_id);
                    absent[0] = provider;
                    return provider;
                });

        if (absent[0] != null) {
            executeHook(CastUtil.cast(absent[0]), Hook::onRegister);
        }
        return old;
    }

    public void unregister(String id, T provider) {
        if (providers.remove(id, provider)) {
            executeHook(provider, Hook::onUnRegister);
        }
    }

    public void unregister(String id) {
        T provider = providers.remove(id);
        if (provider != null) {
            executeHook(provider, Hook::onUnRegister);
        }
    }

    public Disposable addHook(Hook<T> hook) {
        hooks.add(hook);
        return () -> hooks.remove(hook);
    }

    public Optional<T> get(String id) {
        return Optional.ofNullable(providers.get(id));
    }

    public T getNow(String id) {
        return get(id)
                .orElseThrow(() -> new BusinessException.NoStackTrace("Unsupported " + name + ":" + id));
    }

    protected void executeHook(T provider, BiConsumer<Hook<T>, T> executor) {
        if (hooks.isEmpty()) {
            return;
        }
        for (Hook<T> hook : hooks) {
            executor.accept(hook, provider);
        }
    }

}
