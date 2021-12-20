package org.jetlinks.community.device.measurements;

import org.jetlinks.core.metadata.Metadata;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MetadataMeasurementDefinitionTest {

    @Test
    void getId() {
        Metadata metadata = Mockito.mock(Metadata.class);
        MetadataMeasurementDefinition definition = MetadataMeasurementDefinition.of(metadata);
        Mockito.when(metadata.getId())
            .thenReturn("test");
        String id = definition.getId();
        assertNotNull(id);
        Mockito.when(metadata.getName())
            .thenReturn("test");
        String name = definition.getName();
        assertNotNull(name);
    }

    @Test
    void getName() {
    }
}