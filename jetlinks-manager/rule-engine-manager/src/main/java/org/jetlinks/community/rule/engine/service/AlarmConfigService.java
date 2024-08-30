package org.jetlinks.community.rule.engine.service;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.rule.engine.alarm.AlarmHandleInfo;
import org.jetlinks.community.rule.engine.entity.*;
import org.jetlinks.community.rule.engine.enums.AlarmState;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AlarmConfigService extends GenericReactiveCrudService<AlarmConfigEntity, String> {


    private final AlarmRecordService alarmRecordService;

    private final ReactiveRepository<AlarmHandleHistoryEntity, String> handleHistoryRepository;

    private final SceneService sceneService;

    /**
     * 处理告警
     *
     * @param info 告警处理信息
     */
    public Mono<Void> handleAlarm(AlarmHandleInfo info){
        return alarmRecordService
            .changeRecordState(info.getType(), info.getState(), info.getAlarmRecordId())
            .flatMap(total-> {
                if (total > 0){
                    return handleHistoryRepository
                        .save(AlarmHandleHistoryEntity.of(info));
                }else {
                    return Mono.error(new BusinessException("error.the_alarm_record_has_been_processed"));
                }
            })
            .then();
    }


    public Mono<PagerResult<AlarmConfigDetail>> queryDetailPager(QueryParamEntity query) {
        return this
            .queryPager(query)
            .flatMap(result -> Flux
                .fromIterable(result.getData())
                .index()
                .flatMap(tp2 -> this
                    // 转换为详情
                    .convertDetail(tp2.getT2())
                    .map(detail -> Tuples.of(tp2.getT1(), detail)))
                // 重新排序,因为转为详情是异步的可能导致顺序乱掉
                .sort(Comparator.comparingLong(Tuple2::getT1))
                .map(Tuple2::getT2)
                .collectList()
                .map(detail -> PagerResult.of(result.getTotal(), detail, query)));
    }

    /**
     * 转换为详情信息
     *
     * @param entity 告警配置
     * @return 告警配置详情
     */
    private Mono<AlarmConfigDetail> convertDetail(AlarmConfigEntity entity) {
        return sceneService
            .createQuery()
            .and(SceneEntity::getId, "alarm-bind-rule", entity.getId())
            .fetch()
            .collectList()
            .map(sceneInfo -> AlarmConfigDetail
                .of(entity)
                .withScene(sceneInfo));
    }


    //同步场景修改后的数据到告警配置中
    @EventListener
    @Deprecated
    public void handleSceneSaved(EntitySavedEvent<SceneEntity> event) {
        event.async(Mono.defer(() -> Flux
            .fromIterable(event.getEntity())
            .flatMap(this::updateByScene)
            .then()));
    }

    //同步场景修改后的数据到告警配置中
    @EventListener
    @Deprecated
    public void handleSceneSaved(EntityModifyEvent<SceneEntity> event) {

        Map<String, SceneEntity> before = event
            .getBefore()
            .stream()
            .collect(Collectors.toMap(SceneEntity::getId, Function.identity()));

        event.async(
            Flux.fromIterable(event.getAfter())
                .filter(scene -> StringUtils.hasText(scene.getName())
                            && before.get(scene.getId()) != null && (
                            !Objects.equals(before.get(scene.getId()).getName(), scene.getName())
                                ||
                                !Objects.equals(before.get(scene.getId()).getTriggerType(), scene.getTriggerType())
                        )
                )
                .flatMap(this::updateByScene)
        );
    }

    @Deprecated
    public Mono<Void> updateByScene(SceneEntity entity) {
        return createUpdate()
            .set(AlarmConfigEntity::getSceneName, entity.getName())
            .set(AlarmConfigEntity::getSceneTriggerType, entity.getTriggerType())
            .where(AlarmConfigEntity::getSceneId, entity.getId())
            .execute()
            .then();
    }

    public Mono<Void> enable(String id) {
        return createUpdate()
            .set(AlarmConfigEntity::getState, AlarmState.enabled)
            .where(AlarmConfigEntity::getId, id)
            .execute()
            .then();
    }

    public Mono<Void> disable(String id) {
        return createUpdate()
            .set(AlarmConfigEntity::getState, AlarmState.disabled)
            .where(AlarmConfigEntity::getId, id)
            .execute()
            .then();
    }
}
