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
package org.jetlinks.community.notify.dingtalk.corp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.TreeUtils;

import java.util.List;

@Getter
@Setter
public class CorpDepartment {

    @JsonProperty
    @JsonAlias("dept_id")
    private String id;

    @JsonProperty
    private String name;

    @JsonProperty
    @JsonAlias("parent_id")
    private String parentId;

    private List<CorpDepartment> children;

    public static List<CorpDepartment> toTree(List<CorpDepartment> list) {
        return TreeUtils.list2tree(list, CorpDepartment::getId, CorpDepartment::getParentId, CorpDepartment::setChildren);
    }
}
