package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.auth.entity.PositionDetail;
import org.jetlinks.community.auth.entity.PositionEntity;
import org.jetlinks.community.auth.service.PositionService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/position")
@RestController
@Resource(id = "position", name = "职位管理")
@Tag(name = "职位管理")
public class PositionController implements ReactiveServiceCrudController<PositionEntity, String> {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @PatchMapping("/{orgId}/_save")
    @SaveAction
    @Operation(summary = "保存职位信息")
    public Mono<Void> savePosition(@Parameter(description = "组织ID") @PathVariable String orgId,
                                  @Parameter(description = "职位信息")
                                  @RequestBody Flux<PositionDetail> position) {
        return positionService.saveDetail(position.doOnNext(detail -> detail.setOrgId(orgId)));
    }

    @PostMapping("/{orgId}/_create")
    @SaveAction
    @Operation(summary = "新增职位信息")
    public Mono<PositionEntity> addPosition(@Parameter(description = "组织ID") @PathVariable String orgId,
                                  @Parameter(description = "职位信息")
                                  @RequestBody Mono<PositionDetail> position) {

        return positionService.saveDetail(position.doOnNext(detail -> detail.setOrgId(orgId)));
    }



    @PostMapping("/_query/detail/no-paging")
    @QueryAction
    @Operation(summary = "查询职位详情信息(不分页)")
    public Flux<PositionDetail> queryPositionNoPaging(@RequestBody Mono<QueryParamEntity> queryMono) {
        return queryMono
            .doOnNext(param -> param.setPaging(false))
            .flatMapMany(param -> positionService
                .createQuery()
                .orderBy(SortOrder.asc(PositionEntity::getSortIndex))
                .accept(param)
                .fetch()
                .buffer(200)
                .flatMap(positionService::convertPositionDetail)
            );
    }

    @PostMapping("/_query/detail")
    @QueryAction
    @Operation(summary = "查询职位详情信息(分页)")
    public Mono<PagerResult<PositionDetail>> queryPositionPager(@RequestBody Mono<QueryParamEntity> queryMono) {

        return QueryHelper
            .transformPageResult(
                queryPager(queryMono),
                list -> positionService.convertPositionDetail(list).collectList()
            );
    }

    @Override
    public ReactiveCrudService<PositionEntity, String> getService() {
        return positionService;
    }
}
