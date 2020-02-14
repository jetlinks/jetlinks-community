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
