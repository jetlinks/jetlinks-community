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
package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.MultipleI18nSupportEntity;
import org.hswebframework.web.i18n.SingleI18nSupportEntity;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
public class MenuView extends GenericTreeSortSupportEntity<String> implements MultipleI18nSupportEntity {

    /**
     * 在多应用集成运行时使用此字段来区分菜单属于哪个系统
     * 具体标识由各应用前端进行定义
     */
    @Schema(description = "菜单所有者")
    private String owner;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "URL")
    private String url;

    @Schema(description = "父节点")
    private String parentId;

    @Schema(description = "描述")
    private String describe;

    @Schema(description = "按钮")
    private List<ButtonView> buttons;

    @Schema(description = "其他配置")
    private Map<String, Object> options;

    @Schema(description = "子节点")
    private List<MenuView> children;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "数据权限说明")
    private String accessDescription;

    @Schema(description = "是否已授权")
    private boolean granted;

    @Schema(title = "国际化信息定义")
    private Map<String, Map<String, String>> i18nMessages;


    public String getI18nName() {
        return getI18nMessage("name", name);
    }

    public String getI18nName(Locale locale) {
        return getI18nMessage("name", locale, name);
    }

    public static MenuView create() {
        return EntityFactoryHolder.newInstance(MenuView.class, MenuView::new);
    }

    public MenuView withGranted(MenuView granted) {
        if (granted == null) {
            return this;
        }
        this.granted = true;

        this.options = granted.getOptions();
        return this
            .withGrantedButtons(granted.getButtons());
    }

    /**
     * 设置已经赋权的按钮到当前菜单
     *
     * @param grantedButtons 全部按钮
     * @return 原始菜单
     */
    public MenuView withGrantedButtons(Collection<ButtonView> grantedButtons) {
        if (CollectionUtils.isEmpty(grantedButtons) || CollectionUtils.isEmpty(this.buttons)) {
            return this;
        }
        Map<String, ButtonView> grantedButtonMap =
            grantedButtons
                .stream()
                .collect(Collectors.toMap(ButtonView::getId, Function.identity(), (a, b) -> a));

        for (ButtonView button : this.buttons) {
            button.enabled = button.granted = grantedButtonMap.containsKey(button.getId());
        }
        return this;
    }

    public Optional<ButtonView> getButton(String id) {
        if (CollectionUtils.isEmpty(buttons)) {
            return Optional.empty();
        }
        return buttons
            .stream()
            .filter(button -> Objects.equals(id, button.getId()))
            .findFirst();
    }

    public void grantAll() {
        this.granted = true;
        if (CollectionUtils.isNotEmpty(getButtons())) {
            for (ButtonView button : getButtons()) {
                button.granted = true;
            }
        }

    }

    public void resetGrant() {
        this.granted = false;
        if (CollectionUtils.isNotEmpty(getButtons())) {
            for (ButtonView button : getButtons()) {
                button.granted = false;
            }
        }
    }

    @Getter
    @Setter
    public static class ButtonView implements SingleI18nSupportEntity, Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "按钮ID")
        private String id;

        @Schema(description = "按钮名称")
        private String name;

        @Schema(description = "说明")
        private String description;

        @Schema(description = "其他配置")
        private Map<String, Object> options;

        @Schema(description = "是否启用")
        @Deprecated
        private boolean enabled;

        @Schema(description = "是否已授权")
        private boolean granted;

        @Schema(title = "国际化信息定义")
        private Map<String, String> i18nMessages;


        public String getI18nName() {
            return getI18nMessage("name", name);
        }

        public ButtonView() {
        }

        public static ButtonView create() {
            return EntityFactoryHolder.newInstance(ButtonView.class, ButtonView::new);
        }

        public static ButtonView of(String id, String name, String description, Map<String, Object> options) {
            return ButtonView.of(id, name, description, options, true, true);
        }

        public static ButtonView copy(ButtonView button) {
            return ButtonView.of(button.getId(), button.getName(), button.getDescription(), button.getOptions());
        }

        public static ButtonView of(String id, String name, String description, Map<String, Object> options, boolean enabled, boolean granted) {
            ButtonView view = ButtonView.create();
            view.setId(id);
            view.setName(name);
            view.setDescription(description);
            view.setOptions(options);
            view.setEnabled(enabled);
            view.setGranted(granted);
            return view;
        }

        public ButtonView copy() {
            return FastBeanCopier.copy(this, create());
        }

    }

    public static MenuView of(MenuEntity entity) {
        return FastBeanCopier.copy(entity, MenuView.create());
    }

    public static MenuView of(MenuEntity entity, List<MenuBindEntity> binds) {
        MenuView view = of(entity);
        if (binds == null) {
            return view;
        }
        view.granted = true;
        if (MapUtils.isEmpty(view.getOptions())) {
            view.setOptions(new HashMap<>());
        }
        //重新排序
        binds.sort(Comparator.comparing(MenuBindEntity::getPriority));
        Map<String, MenuView.ButtonView> buttons = new LinkedHashMap<>();

        Map<String, ButtonView> buttonViewMap = Optional
            .ofNullable(view.getButtons())
            .map(list -> list
                .stream()
                .collect(Collectors.toMap(ButtonView::getId, Function.identity())))
            .orElse(new HashMap<>());
        for (MenuBindEntity bind : binds) {
            //不合并则清空之前的配置
            if (!bind.getMerge()) {
                view.setOptions(new HashMap<>());
                buttons.clear();
            }
            if (MapUtils.isNotEmpty(bind.getOptions())) {
                view.getOptions().putAll(bind.getOptions());
            }
            //按钮权限
            if (CollectionUtils.isNotEmpty(bind.getButtons())) {
                for (ButtonView button : bind.getButtons()) {
                    ButtonView buttonView = buttonViewMap.get(button.getId());
                    if (button.isGranted() && buttonView != null) {
                        buttons.put(buttonView.getId(), ButtonView.copy(buttonView));
                    }
                }
            }

        }
        view.setButtons(new ArrayList<>(buttons.values()));
        return view;

    }
}
