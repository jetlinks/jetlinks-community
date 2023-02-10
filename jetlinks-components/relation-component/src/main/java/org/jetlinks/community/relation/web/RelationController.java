package org.jetlinks.community.relation.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.relation.configuration.RelationProperties;
import org.jetlinks.core.things.relation.ObjectType;
import org.jetlinks.core.things.relation.RelationManager;
import org.jetlinks.community.relation.entity.RelationEntity;
import org.jetlinks.community.relation.service.RelationService;
import org.jetlinks.community.relation.service.request.SaveRelationRequest;
import org.jetlinks.community.relation.service.response.RelatedInfo;
import org.jetlinks.community.web.response.ValidationResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/relation")
@Resource(id = "relation", name = "关系管理")
@AllArgsConstructor
@Getter
public class RelationController implements ReactiveServiceCrudController<RelationEntity, String> {

    private final RelationProperties properties;

    private final RelationService service;

    private final RelationManager relationManager;

    @GetMapping("/{type}/{id}/related")
    @Operation(summary = "查询指定类型数据的关系信息")
    @QueryAction
    public Flux<RelatedInfo> getRelationInfo(@PathVariable String type, @PathVariable String id) {
        //数据权限控制
        return service.getRelationInfo(type, id);
    }

    @PatchMapping("/{type}/{id}/_bind")
    @Operation(summary = "保存指定类型数据的关系信息")
    @SaveAction
    public Mono<Void> saveRelation(@PathVariable String type,
                                   @PathVariable String id,
                                   @RequestBody Flux<SaveRelationRequest> requestFlux) {
        return service.saveRelated(type, id, requestFlux);
    }

    @GetMapping("/types")
    @Authorize(merge = false)
    @Operation(summary = "获取所有的关系对象类型")
    public Flux<ObjectTypeInfo> getTypes() {
        return relationManager
            .getObjectTypes()
            .map(type -> ObjectTypeInfo.of(type, properties.getRelatable(type.getId())));
    }

    @GetMapping("/{type}/relations")
    @Authorize(merge = false)
    @Operation(summary = "获取指定类型的关系定义")
    public Flux<ObjectTypeInfo> getTypes(@PathVariable String type) {
        return relationManager
            .getObjectType(type)
            .flatMapIterable(ObjectType::getRelatedTypes)
            .map(_type -> ObjectTypeInfo.of(_type, properties.getRelatable(_type.getId())));
    }

    @GetMapping("/_validate")
    @QueryAction
    @Operation(summary = "验证关系标识是否合法")
    public Mono<ValidationResult> relationValidate2(@RequestParam @Parameter(description = "对象类型") String objectType,
                                                    @RequestParam @Parameter(description = "关系标识") String relation,
                                                    @RequestParam @Parameter(description = "目标对象类型") String targetType) {
        return LocaleUtils.currentReactive()
            .flatMap(locale -> {
                RelationEntity entity = new RelationEntity();
                entity.setObjectType(objectType);
                entity.setRelation(relation);
                entity.setTargetType(targetType);
                entity.generateId();

                return service.findById(entity.getId())
                    .map(relationEntity -> ValidationResult.error(
                        LocaleUtils.resolveMessage("error.relation_ID_already_exists", locale)));
            })
            .defaultIfEmpty(ValidationResult.success());
    }

    @Getter
    @Setter
    public static class ObjectTypeInfo {
        @Schema(description = "类型ID")
        private String id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "说明")
        private String description;

        @Schema(description = "可建立关系的其他类型ID")
        private List<String> relatable;

        public static ObjectTypeInfo of(ObjectType type, List<String> relatable) {
            ObjectTypeInfo info = new ObjectTypeInfo();
            info.setId(type.getId());
            info.setName(type.getName());
            info.setDescription(type.getDescription());
            info.setRelatable(relatable);
            return info;
        }
    }

}
