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
package org.jetlinks.community.iotdb;

import org.jetlinks.community.iotdb.things.IoTDBColumnModeStrategy;
import org.jetlinks.community.iotdb.things.IoTDBRowModeStrategy;
import org.jetlinks.community.iotdb.things.IoTDBThingDataConfiguration;
import org.jetlinks.community.iotdb.timeseries.IoTDBTimeSeriesConfiguration;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.things.ThingsRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IoTDBAutoConfigurationTest {

    @Test
    void shouldRegisterTimeSeriesAndThingStrategiesWhenEnabled() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                IoTDBConfiguration.class,
                IoTDBThingDataConfiguration.class,
                IoTDBTimeSeriesConfiguration.class))
            .withBean(IoTDBOperations.class, () -> mock(IoTDBOperations.class))
            .withBean(ThingsRegistry.class, () -> mock(ThingsRegistry.class))
            .withPropertyValues(
                "iotdb.enabled=true",
                "iotdb.database=jetlinks",
                "iotdb.username=test",
                "iotdb.password=test")
            .run(context -> assertThat(context)
                .hasSingleBean(IoTDBOperations.class)
                .hasSingleBean(TimeSeriesManager.class)
                .hasSingleBean(IoTDBRowModeStrategy.class)
                .hasSingleBean(IoTDBColumnModeStrategy.class));
    }
}
