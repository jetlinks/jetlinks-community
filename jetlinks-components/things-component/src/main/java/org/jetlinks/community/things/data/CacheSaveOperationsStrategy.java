package org.jetlinks.community.things.data;

import org.jetlinks.community.things.data.operations.SaveOperations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CacheSaveOperationsStrategy implements ThingsDataRepositoryStrategy {


    private final Map<OperationsContext, SaveOperations> caches = new ConcurrentHashMap<>();

    @Override
    public final SaveOperations opsForSave(OperationsContext context) {
        //save可能是频繁操作,使用cache减少对象创建
        return caches.computeIfAbsent(context, this::createOpsForSave);
    }


    protected abstract SaveOperations createOpsForSave(OperationsContext context);
}
