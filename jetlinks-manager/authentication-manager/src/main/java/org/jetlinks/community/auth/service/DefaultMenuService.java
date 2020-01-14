package org.jetlinks.community.auth.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.crud.service.ReactiveTreeSortEntityService;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wangzheng
 * @since 1.0
 */
@Service
public class DefaultMenuService
        extends GenericReactiveCrudService<MenuEntity, String>
        implements ReactiveTreeSortEntityService<MenuEntity, String> {
    @Override
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.MD5;
    }

    @Override
    public void setChildren(MenuEntity menuEntity, List<MenuEntity> children) {
        menuEntity.setChildren(children);
    }

    @Override
    public List<MenuEntity> getChildren(MenuEntity menuEntity) {
        return menuEntity.getChildren();
    }
}
