package org.jetlinks.community.device.entity.excel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceInstanceImportExportEntityTest {

    @Test
    void get() {
        DeviceInstanceImportExportEntity entity = new DeviceInstanceImportExportEntity();
        entity.setId("test");
        entity.setName("test");
        entity.setProductName("test");
        entity.setDescribe("test");
        entity.setParentId("test");

        String id = entity.getId();
        assertNotNull(id);
        String name = entity.getName();
        assertNotNull(name);
        String parentId = entity.getParentId();
        assertNotNull(parentId);
        String productName = entity.getProductName();
        assertNotNull(productName);
        String describe = entity.getDescribe();
        assertNotNull(describe);
    }
}