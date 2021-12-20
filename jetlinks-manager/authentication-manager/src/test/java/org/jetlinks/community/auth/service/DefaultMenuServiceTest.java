package org.jetlinks.community.auth.service;

import org.jetlinks.community.auth.entity.MenuEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultMenuServiceTest {

  @Test
  void getIDGenerator() {
      DefaultMenuService defaultMenuService = new DefaultMenuService();
      defaultMenuService.getIDGenerator();
  }

  @Test
  void setChildren() {
      MenuEntity menuEntity = new MenuEntity();
      List<MenuEntity> children = new ArrayList<>();
      DefaultMenuService defaultMenuService = new DefaultMenuService();
      defaultMenuService.setChildren(menuEntity,children);
  }

  @Test
  void getChildren() {
      MenuEntity menuEntity = new MenuEntity();
      List<MenuEntity> children = new ArrayList<>();
      MenuEntity menuEntity1 = new MenuEntity();
      children.add(menuEntity1);
      DefaultMenuService defaultMenuService = new DefaultMenuService();
      defaultMenuService.setChildren(menuEntity,children);
      List<MenuEntity> children1 = defaultMenuService.getChildren(menuEntity);
      assertNotNull(children1);
      assertEquals(1,children1.size());
  }
}