package org.jetlinks.community.rule.engine.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.web.dictionary.entity.DictionaryEntity;
import org.hswebframework.web.dictionary.entity.DictionaryItemEntity;
import org.jetlinks.community.dictionary.DictionaryConstants;
import org.jetlinks.community.dictionary.DictionaryInitInfo;
import org.jetlinks.community.rule.engine.enums.AlarmHandleType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@Slf4j
@Component
public class AlarmHandleTypeDictInit implements DictionaryInitInfo {

    public static final String DICT_ID = "alarm_handle_type";

    @Override
    public Collection<DictionaryEntity> getDict() {

        DictionaryEntity entity = new DictionaryEntity();
        entity.setId(DICT_ID);
        entity.setName("告警处理类型");
        entity.setClassified(DictionaryConstants.CLASSIFIED_SYSTEM);
        entity.setStatus((byte) 1);

        List<DictionaryItemEntity> items = new ArrayList<>();

        int index = 1;
        for (AlarmHandleType type : AlarmHandleType.values()) {
            DictionaryItemEntity item = new DictionaryItemEntity();
            item.setId(DigestUtils.md5Hex(DICT_ID + type.getValue()));
            item.setValue(type.getValue());
            item.setText(type.getText());
            item.setDictId(DICT_ID);
            item.setStatus((byte) 1);
            item.setOrdinal(index++);
            items.add(item);
        }

        entity.setItems(items);

        return Collections.singletonList(entity);

    }
}
