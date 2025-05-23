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
package org.jetlinks.community.things;

import org.jetlinks.community.things.data.operations.SaveOperations;
import org.jetlinks.community.things.data.operations.TemplateOperations;
import org.jetlinks.community.things.data.operations.ThingOperations;
import reactor.core.publisher.Mono;

/**
 * 物数据仓库,用于保存和查询物模型相关数据: 属性,事件,以及日志
 *
 * @author zhouhao
 * @since 2.0
 */
public interface ThingsDataRepository {

    /**
     * @return 返回保存操作接口, 用于对物数据进行保存
     */
    SaveOperations opsForSave();

    /**
     * 返回物操作接口, 基于物实例进行数据操作,如:查询单个物实例的属性历史.
     *
     * @param thingType 物类型
     * @param thingId   物实例ID
     * @return 操作接口
     */
    Mono<ThingOperations> opsForThing(String thingType, String thingId);

    /**
     * 返回物模版操作接口,基于物模版进行数据操作,如: 查询物模版下所有物实例的数据.
     *
     * @param thingType  物类型
     * @param templateId 物模版ID
     * @return 操作接口
     */
    Mono<TemplateOperations> opsForTemplate(String thingType, String templateId);

}
