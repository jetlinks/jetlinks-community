package org.jetlinks.community.auth.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.jetlinks.community.auth.entity.PositionDetail;

import java.util.List;
import java.util.Map;

/**
 * @author gyl
 * @since 2.2
 */
@Getter
@Setter
public class OrganizationDetail {

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "说明")
    private String describe;

    @Schema(description = "其他配置")
    private Map<String, Object> properties;

    @Schema(description = "父节点ID")
    private String parentId;

    @Schema(description = "树结构路径")
    private String path;

    @Schema(description = "排序序号")
    private Long sortIndex;

    @Schema(description = "树层级")
    private Integer level;

    private String creatorId;

    private Long createTime;

    private String fullName;

    @Schema(description = "机构ID(只能由数字,字母,下划线和中划线组成)")
    public String id;

    @Schema(description = "本层级以及子层级所有人数")
    private long memberCount = 0L;


    private List<OrganizationDetail> children;

    @Schema(description = "岗位信息")
    private List<PositionDetail> positions;

    public static final String INTERVAL_CHARACTER = "/";

    public void addParentFullName(OrganizationEntity rootParent) {
        this.setFullName(generateFullName(rootParent) + INTERVAL_CHARACTER + this.getName());
    }

    public static String generateFullName(OrganizationEntity root) {
        String fullName = root.getName();
        if (CollectionUtils.isEmpty(root.getChildren())) {
            return fullName;
        }
        if (root.getChildren().size() == 1) {
            return fullName + INTERVAL_CHARACTER + generateFullName(root.getChildren().get(0));
        }
        return fullName;
    }

    public static OrganizationDetail of(){
        return EntityFactoryHolder.newInstance(OrganizationDetail.class,OrganizationDetail::new);
    }

    public static OrganizationDetail from(OrganizationEntity entity) {
        return entity.copyTo(of());
    }


    /**
     * 填充本层级人数
     */
    public void fillMemberCount(int memberCount) {
        this.memberCount =  memberCount+ this.memberCount;
    }

    /**
     * 添加子级人数
     * @param subMemberCount 子级人数
     */
    public void addMemberCount(long  subMemberCount) {
        this.setMemberCount(memberCount + subMemberCount);
    }
}
