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
package org.jetlinks.community.doc;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.Term;

import java.util.List;

/**
 * 文档专用,描述仅有查询功能的动态查询参数
 *
 * @author zhouhao
 * @since 1.5
 * @see org.hswebframework.web.api.crud.entity.QueryParamEntity
 */
@Getter
@Setter
public class QueryConditionOnly {

    @Schema(description = "where条件表达式,与terms参数不能共存.语法: name = 张三 and age > 16")
    private String where;

    @Schema(description = "查询条件集合")
    private List<Term> terms;

}
