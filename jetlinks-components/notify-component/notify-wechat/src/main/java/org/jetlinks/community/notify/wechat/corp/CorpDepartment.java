package org.jetlinks.community.notify.wechat.corp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.TreeUtils;

import java.util.List;

/**
 * 企业微信部门信息
 *
 * @author zhouhao
 * @see <a href="https://developer.work.weixin.qq.com/document/path/90208">企业微信-获取部门列表</a>
 * @since 2.0
 */
@Getter
@Setter
public class CorpDepartment implements Comparable<CorpDepartment> {

    @JsonProperty
    @Schema(description = "部门ID")
    private String id;

    @JsonProperty
    @Schema(description = "部门名称")
    private String name;

    @Schema(description = "英文名称")
    @JsonProperty
    @JsonAlias("name_en")
    private String nameEn;

    @Schema(description = "部门负责人的UserID")
    @JsonProperty
    @JsonAlias("department_leader")
    private List<String> departmentLeader;

    @Schema(description = "父部门id")
    @JsonProperty
    @JsonAlias("parentid")
    private String parentId;

    @Schema(description = "父部门id")
    private long order;

    private List<CorpDepartment> children;

    public static List<CorpDepartment> toTree(List<CorpDepartment> list) {
        return TreeUtils.list2tree(list,
                                   CorpDepartment::getId,
                                   CorpDepartment::getParentId,
                                   CorpDepartment::setChildren);
    }

    @Override
    public int compareTo(CorpDepartment target) {
        return Long.compare(this.order, target.getOrder());
    }
}
