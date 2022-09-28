package org.jetlinks.community.rule.engine.alarm;

import org.jetlinks.community.rule.engine.scene.SceneData;
import org.jetlinks.reactor.ql.utils.CastUtils;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author bestfeng
 */

public class ProductAlarmTarget implements AlarmTarget {

    @Override
    public String getType() {
        return "product";
    }

    @Override
    public String getName() {
        return "产品";
    }

    @Override
    public Flux<AlarmTargetInfo> convert(SceneData data) {
        Map<String, Object> output = data.getOutput();
        String productId = CastUtils.castString(output.get("productId"));
        String productName = CastUtils.castString(output.getOrDefault("productName", productId));
        return Flux.just(AlarmTargetInfo.of(productId, productName, getType()));
    }

}
