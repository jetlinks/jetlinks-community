package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.hswebframework.web.bean.FastBeanCopier;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MenuView extends GenericTreeSortSupportEntity<String> {

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "URL")
    private String url;

    @Schema(description = "父节点")
    private String parentId;

    @Schema(description = "按钮")
    private List<ButtonView> buttons;

    @Schema(description = "其他配置")
    private Map<String, Object> options;

    @Schema(description = "子节点")
    private List<MenuView> children;

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class ButtonView{

        @Schema(description = "按钮ID")
        private String id;

        @Schema(description = "按钮名称")
        private String name;

        @Schema(description = "其他配置")
        private Map<String,Object> options;
    }

    public static MenuView of(MenuEntity entity){
        return FastBeanCopier.copy(entity,new MenuView());
    }
}
