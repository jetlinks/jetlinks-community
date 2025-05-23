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
package org.jetlinks.community.dashboard.web.response;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.dashboard.Dashboard;
import reactor.core.publisher.Mono;

import java.util.List;

@Getter
@Setter
public class DashboardInfo {

    private String id;

    private String name;

    private List<ObjectInfo> objects;

    public static Mono<DashboardInfo> of(Dashboard dashboard) {
        return dashboard.getObjects()
            .map(ObjectInfo::of)
            .collectList()
            .map(list -> {
                DashboardInfo dashboardInfo = new DashboardInfo();
                dashboardInfo.setId(dashboard.getDefinition().getId());
                dashboardInfo.setName(dashboard.getDefinition().getName());
                dashboardInfo.setObjects(list);
                return dashboardInfo;
            });

    }

}
