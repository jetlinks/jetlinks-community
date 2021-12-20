package org.jetlinks.community.device.timeseries;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FixedPropertiesTimeSeriesMetadataTest {

  @Test
  void getMetric() {
      String test = new FixedPropertiesTimeSeriesMetadata("test", new ArrayList<>())
          .getMetric().getId();
      assertNotNull(test);
      assertEquals("properties_test",test);
  }

  @Test
  void getProperties() {
      Integer test = new FixedPropertiesTimeSeriesMetadata("test", new ArrayList<>())
          .getProperties().size();
      assertNotNull(test);
      assertEquals(4,test);
  }
}