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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "iotdb")
public class IoTDBProperties {

    @NotBlank
    private String database;

    private List<String> nodeUrls = new ArrayList<>(List.of("localhost:6667"));

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @Min(1)
    private int maxSize = 5;

    @Min(1)
    private long queryTimeoutInMs = 10_000;

    @Min(1)
    private int fetchSize = 1_000;

    @Min(1)
    private int connectionTimeoutInMs = 5_000;

    @Min(1)
    private int writeBatchSize = 500;
}
