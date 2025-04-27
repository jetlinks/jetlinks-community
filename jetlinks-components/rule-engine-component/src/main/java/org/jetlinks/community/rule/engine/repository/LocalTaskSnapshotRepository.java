package org.jetlinks.community.rule.engine.repository;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.rule.engine.entity.TaskSnapshotEntity;
import org.jetlinks.rule.engine.api.task.TaskSnapshot;
import org.jetlinks.rule.engine.cluster.TaskSnapshotRepository;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public class LocalTaskSnapshotRepository implements TaskSnapshotRepository {

    private final ReactiveRepository<TaskSnapshotEntity, String> repository;

    public LocalTaskSnapshotRepository(ReactiveRepository<TaskSnapshotEntity, String> repository) {
        this.repository = repository;
    }

    @Override
    public Flux<TaskSnapshot> findAllTask() {
        return repository
            .createQuery()
            .fetch()
            .map(TaskSnapshotEntity::toSnapshot);
    }


    @Override
    public Flux<TaskSnapshot> findByInstanceId(String instanceId) {
        return repository
            .createQuery()
            .where(TaskSnapshotEntity::getInstanceId, instanceId)
            .fetch()
            .map(TaskSnapshotEntity::toSnapshot);
    }

    @Override
    public Flux<TaskSnapshot> findByWorkerId(String workerId) {
        return repository
            .createQuery()
            .where(TaskSnapshotEntity::getWorkerId, workerId)
            .fetch()
            .map(TaskSnapshotEntity::toSnapshot);
    }

    @Override
    public Flux<TaskSnapshot> findBySchedulerId(String schedulerId) {
        return repository
            .createQuery()
            .where(TaskSnapshotEntity::getSchedulerId, schedulerId)
            .fetch()
            .map(TaskSnapshotEntity::toSnapshot);
    }

    @Override
    public Flux<TaskSnapshot> findBySchedulerIdNotIn(Collection<String> schedulerId) {
        return repository
            .createQuery()
            .where()
            .notIn(TaskSnapshotEntity::getSchedulerId, schedulerId)
            .fetch()
            .map(TaskSnapshotEntity::toSnapshot);
    }

    @Override
    public Flux<TaskSnapshot> findByInstanceIdAndWorkerId(String instanceId, String workerId) {
        return repository
            .createQuery()
            .where(TaskSnapshotEntity::getInstanceId, instanceId)
            .and(TaskSnapshotEntity::getWorkerId, workerId)
            .fetch()
            .map(TaskSnapshotEntity::toSnapshot);
    }

    @Override
    public Flux<TaskSnapshot> findByInstanceIdAndNodeId(String instanceId, String nodeId) {
        return repository
            .createQuery()
            .where(TaskSnapshotEntity::getInstanceId, instanceId)
            .and(TaskSnapshotEntity::getNodeId, nodeId)
            .fetch()
            .map(TaskSnapshotEntity::toSnapshot);
    }

    @Override
    public Mono<Void> saveTaskSnapshots(Publisher<TaskSnapshot> snapshots) {
        return Flux
            .from(snapshots)
            .map(TaskSnapshotEntity::of)
            .buffer(200)
            .concatMap(repository::save)
            .then();
    }

    @Override
    public Mono<Void> removeTaskByInstanceId(String instanceId) {
        return repository
            .createDelete()
            .where(TaskSnapshotEntity::getInstanceId, instanceId)
            .execute()
            .then();
    }

    @Override
    public Mono<Void> removeTaskByInstanceIdAndNodeId(String instanceId, String nodeId) {
        return repository
            .createDelete()
            .where(TaskSnapshotEntity::getInstanceId, instanceId)
            .and(TaskSnapshotEntity::getNodeId, nodeId)
            .execute()
            .then();
    }

    @Override
    public Mono<Void> removeTaskById(String id) {
        return repository
            .deleteById(id)
            .then();
    }
}
