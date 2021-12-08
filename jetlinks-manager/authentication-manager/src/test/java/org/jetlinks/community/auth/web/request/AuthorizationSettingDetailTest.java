package org.jetlinks.community.auth.web.request;

import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.authorization.simple.SimpleDimension;
import org.hswebframework.web.authorization.simple.SimpleUser;
import org.hswebframework.web.system.authorization.api.entity.AuthorizationSettingEntity;
import org.hswebframework.web.system.authorization.api.entity.DataAccessEntity;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationSettingDetailTest {
    private static final String ID = "test";

    @Test
    void hasPermission() {
        AuthorizationSettingDetail detail = new AuthorizationSettingDetail();
        detail.setPermissionList(new ArrayList<>());
        List<String> actions = new ArrayList<>();
        assertFalse(detail.hasPermission(ID,actions));

        AuthorizationSettingDetail detail1 = new AuthorizationSettingDetail();
        List<AuthorizationSettingDetail.PermissionInfo> permissionList = new ArrayList<>();
        AuthorizationSettingDetail.PermissionInfo permissionInfo = new AuthorizationSettingDetail.PermissionInfo();
        permissionInfo.setId(ID);
        Set<String> set = new HashSet<>();
        permissionInfo.setActions(set);
        permissionInfo.setDataAccess(new ArrayList<>());
        permissionInfo.setFieldAccess(new ArrayList<>());
        permissionList.add(permissionInfo);
        detail1.setPermissionList(permissionList);

        assertNotNull(permissionInfo.getFieldAccess());
        assertNotNull(permissionInfo.getDataAccess());

        assertTrue(detail1.hasPermission(ID,actions));
    }


    @Test
    void fromEntity() {
        List<AuthorizationSettingEntity> entities = new ArrayList<>();
        AuthorizationSettingEntity entity = new AuthorizationSettingEntity();
        entity.setId(ID);
        entity.setPermission("test");
        entity.setDimensionType("type");
        entity.setDimensionTarget("target");
        Set<String> actions=new HashSet<>();
        entity.setActions(actions);
        List<DataAccessEntity> dataAccesses = new ArrayList<>();
        DataAccessEntity dataAccessEntity = new DataAccessEntity();
        dataAccessEntity.setType("DENY_FIELDS");
        Map<String, Object> config = new HashMap<>();
        Set<String> fields = new HashSet<>();
        fields.add("field_test");
        config.put("fields",fields);
        dataAccessEntity.setConfig(config);
        dataAccesses.add(dataAccessEntity);
        entity.setDataAccesses(dataAccesses);
        entities.add(entity);
        AuthorizationSettingDetail detail = AuthorizationSettingDetail.fromEntity(entities);
        assertNotNull(detail);
    }
    @Test
    void fromEntity1() {
        List<AuthorizationSettingEntity> entities = new ArrayList<>();
        AuthorizationSettingEntity entity = new AuthorizationSettingEntity();
        entity.setId(ID);
        entity.setPermission("test");
        entity.setDimensionType("type");
        entity.setDimensionTarget("target");
        Set<String> actions=new HashSet<>();
        entity.setActions(actions);
        List<DataAccessEntity> dataAccesses = new ArrayList<>();
        DataAccessEntity dataAccessEntity = new DataAccessEntity();
        dataAccessEntity.setType("DIMENSION_SCOPE");
        Map<String, Object> config = new HashMap<>();
        Set<String> fields = new HashSet<>();
        fields.add("field_test");
        config.put("fields",fields);
        dataAccessEntity.setConfig(config);
        dataAccesses.add(dataAccessEntity);
        entity.setDataAccesses(dataAccesses);
        entities.add(entity);
        AuthorizationSettingDetail detail = AuthorizationSettingDetail.fromEntity(entities);
        assertNotNull(detail);
    }

    @Test
    void toEntity() {
        AuthorizationSettingDetail detail = new AuthorizationSettingDetail();
        List<AuthorizationSettingDetail.PermissionInfo> permissionList = new ArrayList<>();
        AuthorizationSettingDetail.PermissionInfo permissionInfo = new AuthorizationSettingDetail.PermissionInfo();
        permissionInfo.setId(ID);
        Set<String> actions = new HashSet<>();
        actions.add("test");
        permissionInfo.setActions(actions);
        //设置permissionInfo的fieldAccess
        List<AuthorizationSettingDetail.FieldAccess> fieldAccessList = new ArrayList<>();
        AuthorizationSettingDetail.FieldAccess fieldAccess = new AuthorizationSettingDetail.FieldAccess();
        fieldAccess.setName("t");
        fieldAccess.setAction(actions);
        fieldAccessList.add(fieldAccess);
        permissionInfo.setFieldAccess(fieldAccessList);

        assertNotNull(fieldAccess.getName());

        //设置permissionInfo的dataAccess
        List<AuthorizationSettingDetail.DataAccess> dataAccess = new ArrayList<>();
        AuthorizationSettingDetail.DataAccess access = new AuthorizationSettingDetail.DataAccess();
        access.setActions(new HashSet<>());
        dataAccess.add(access);
        permissionInfo.setDataAccess(dataAccess);

        permissionList.add(permissionInfo);
        detail.setPermissionList(permissionList);
        detail.setTargetId("targetId");
        detail.setTargetType("targetType");
        detail.setMerge(true);
        detail.setPriority(1);

        SimpleDimension dimension = new SimpleDimension();
        dimension.setId("uid");
        dimension.setName("name");
        dimension.setType(DefaultDimensionType.user);
        List<AuthorizationSettingEntity> list = detail.toEntity(dimension);
        assertNotNull(list);

        Set<String> D_Actions = new HashSet<>();
        D_Actions.add("test");
        access.setActions(D_Actions);
        access.setType("org");
        access.setConfig(new HashMap<>());
        assertNotNull(access.getActions());
        List<AuthorizationSettingEntity> list1 = detail.toEntity(dimension);
        assertNotNull(list1);
    }

}