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
