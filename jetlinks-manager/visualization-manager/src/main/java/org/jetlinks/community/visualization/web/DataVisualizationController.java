package org.jetlinks.community.visualization.web;

import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.visualization.entity.DataVisualizationEntity;
import org.jetlinks.community.visualization.service.DataVisualizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/visualization")
@Resource(id = "visualization", name = "可视化管理")
public class DataVisualizationController implements ReactiveServiceCrudController<DataVisualizationEntity, String> {

    private final DataVisualizationService visualizationService;

    public DataVisualizationController(DataVisualizationService visualizationService) {
        this.visualizationService = visualizationService;
    }

    @Override
    public DataVisualizationService getService() {
        return visualizationService;
    }

    @GetMapping("/{type}/{target}")
    @QueryAction
    public Mono<DataVisualizationEntity> getByTypeAndTarget(@PathVariable String type,
                                                            @PathVariable String target) {
        return visualizationService.createQuery()
            .where(DataVisualizationEntity::getType, type)
            .and(DataVisualizationEntity::getTarget, target)
            .fetchOne()
            .defaultIfEmpty(DataVisualizationEntity.newEmpty(type, target));
    }

}
