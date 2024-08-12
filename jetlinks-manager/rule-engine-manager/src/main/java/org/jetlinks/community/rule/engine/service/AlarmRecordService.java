package org.jetlinks.community.rule.engine.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.rule.engine.entity.AlarmRecordEntity;
import org.jetlinks.community.rule.engine.enums.AlarmRecordState;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class AlarmRecordService extends GenericReactiveCrudService<AlarmRecordEntity, String> {

    /**
     * 修改告警记录状态
     * @param type 告警处理类型
     * @param state 修改后的告警记录状态
     * @param id 告警记录ID
     * @return
     */
    public Mono<Integer> changeRecordState(String type,
                                           AlarmRecordState state,
                                           String id) {
        return createUpdate()
            .set(AlarmRecordEntity::getState, state)
            .set(AlarmRecordEntity::getHandleTime, System.currentTimeMillis())
            .set(AlarmRecordEntity::getHandleType, type)
            .where(AlarmRecordEntity::getId, id)
            .not(AlarmRecordEntity::getState, state)
            .execute();
    }


}
