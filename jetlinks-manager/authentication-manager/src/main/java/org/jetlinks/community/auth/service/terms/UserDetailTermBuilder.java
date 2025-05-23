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
package org.jetlinks.community.auth.service.terms;

import org.jetlinks.community.terms.SubTableTermFragmentBuilder;
import org.springframework.stereotype.Component;

/**
 * 根据用户详情条件 查询用户.
 * <p>
 * 将用户详情的条件（手机号、邮箱）嵌套到此条件中
 * <pre>{@code
 * "terms": [
 *      {
 *          "column": "id$user-detail",
 *          "value": [
 *              {
 *              "column": "telephone",
 *              "termType": "like",
 *              "value": "%888%"
 *              },
 *              {
 *              "column": "email",
 *              "termType": "like",
 *              "value": "%123%"
 *              }
 *          ]
 *      }
 *  ]
 * }</pre>
 * @author zhangji 2022/6/29
 */
@Component
public class UserDetailTermBuilder extends SubTableTermFragmentBuilder {
    public UserDetailTermBuilder() {
        super("user-detail", "按用户详情查询");
    }

    @Override
    protected String getTableAlias() {
        return "_detail";
    }

    @Override
    protected String getSubTableName() {
        return "s_user_detail";
    }

}
