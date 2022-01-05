package org.jetlinks.community.visualization.entity;

import org.jetlinks.community.visualization.enums.DataVisualizationState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataVisualizationEntityTest {

    @Test
    void applyId() {
        DataVisualizationEntity entity = new DataVisualizationEntity();
        entity.setId("id");
        entity.setTarget("id");
        entity.setName("id");
        entity.setType("id");
        entity.setDescription("id");
        entity.setMetadata("id");
        entity.setState(DataVisualizationState.enabled);

        assertNotNull(entity.getId());
        assertNotNull(entity.getTarget());
        assertNotNull(entity.getType());
        assertNotNull(entity.getName());
        assertNotNull(entity.getDescription());
        assertNotNull(entity.getMetadata());
        assertNotNull(entity.getState());

        DataVisualizationEntity.newEmpty("type","target");
    }

}