/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.rule.engine.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.web.dictionary.entity.DictionaryEntity;
import org.hswebframework.web.dictionary.entity.DictionaryItemEntity;
import org.hswebframework.web.i18n.LocaleUtils;
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
        entity.putI18nName("message.handle.type.dict.name", LocaleUtils.getSupportLocales());
        entity.setStatus((byte) 1);

        List<DictionaryItemEntity> items = new ArrayList<>();

        int index = 1;
        for (AlarmHandleType type : AlarmHandleType.values()) {
            DictionaryItemEntity item = new DictionaryItemEntity();
            item.setId(DigestUtils.md5Hex(DICT_ID + type.getValue()));
            item.setName(type.getValue());
            item.setValue(type.getValue());
            item.setText(type.getText());
            item.putI18nText("message.handle.type.dict.item." + type.name(), LocaleUtils.getSupportLocales());
            item.setDictId(DICT_ID);
            item.setStatus((byte) 1);
            item.setOrdinal(index++);
            items.add(item);
        }

        entity.setItems(items);

        return Collections.singletonList(entity);

    }
}
