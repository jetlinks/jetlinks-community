package org.jetlinks.community.device.function;

import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.ezorm.core.Conditional;
import org.hswebframework.ezorm.core.NestConditional;
import org.jetlinks.core.things.relation.ObjectSpec;
import org.jetlinks.core.things.relation.RelatedObject;
import org.jetlinks.core.things.relation.RelationObject;
import org.jetlinks.core.things.relation.RelationSpec;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.relation.RelationManagerHolder;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProvider;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class RelationDeviceSelectorProvider implements DeviceSelectorProvider {
    @Override
    public String getProvider() {
        return "relation";
    }

    @Override
    public String getName() {
        return "按关系";
    }

    @Override
    public <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(DeviceSelectorSpec source,
                                                                              Map<String, Object> ctx,
                                                                              NestConditional<T> conditional) {

        return this
            .applyCondition(
                source.resolve(ctx).map(String::valueOf),
                Flux.fromIterable(source.getSelectorValues()).map(RelationSpec::of),
                conditional
            );
    }

    @Override
    public <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(List<?> args,
                                                                              NestConditional<T> conditional) {
        //第一个参数 为设备ID,其余参数为关系
        Flux<RelationSpec> relations = Flux
            .fromIterable(args)
            .skip(1)
            .map(RelationSpec::of);
        return applyCondition(Flux.just(String.valueOf(args.get(0))), relations, conditional);
    }

    public <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(Flux<String> source,
                                                                              Flux<RelationSpec> relations,
                                                                              NestConditional<T> conditional) {
        return source
            .flatMap(deviceId -> relations
                .flatMap(spec -> {
                    // deviceId@device:relation@user
                    ObjectSpec objectSpec = new ObjectSpec();
                    objectSpec.setObjectType(RelationObjectProvider.TYPE_DEVICE);
                    objectSpec.setObjectId(deviceId);
                    objectSpec.setRelated(spec);
                    return RelationManagerHolder
                        .getObjects(objectSpec)
                        .flatMap(obj -> {
                            //已经选择到了设备
                            if (obj.getType().equals(RelationObjectProvider.TYPE_DEVICE)) {
                                return Mono.just(obj);
                            }
                            return obj
                                //反转获取,表示获取与上一个关系相同的设备
                                .relations(true)
                                .get(RelationObjectProvider.TYPE_DEVICE, ((RelatedObject) obj).getRelation());
                        });
                }))
            .map(RelationObject::getId)
            .collectList()
            .doOnNext(deviceIdList -> {
                if (CollectionUtils.isNotEmpty(deviceIdList)){
                    conditional.in(DeviceInstanceEntity::getId, deviceIdList);
                }else {
                    conditional.isNull(DeviceInstanceEntity::getId);
                }
            })
            .thenReturn(conditional);
    }
}
