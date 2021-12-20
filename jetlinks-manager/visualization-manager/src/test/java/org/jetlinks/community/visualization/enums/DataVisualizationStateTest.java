package org.jetlinks.community.visualization.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataVisualizationStateTest {

  @Test
  void getValue() {
      assertNotNull(DataVisualizationState.enabled.getValue());
      assertNotNull(DataVisualizationState.enabled.getText());
      assertNotNull(DataVisualizationState.disabled.getValue());
      assertNotNull(DataVisualizationState.disabled.getText());
  }
}