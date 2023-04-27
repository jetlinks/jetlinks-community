package org.jetlinks.community.rule.engine.alarm;

import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author bestfeng
 */

public class ProductAlarmTarget extends AbstractAlarmTarget {

    @Override
    public String getType() {
        return "product";
    }

    @Override
    public String getName() {
        return "产品";
    }

    @Override
    public Flux<AlarmTargetInfo> doConvert(AlarmData data) {
        Map<String, Object> output = data.getOutput();
        String productId = AbstractAlarmTarget.getFromOutput("productId", output).map(String::valueOf).orElse(null);
        String productName = AbstractAlarmTarget.getFromOutput("productName", output).map(String::valueOf).orElse(productId);

        return Flux.just(AlarmTargetInfo.of(productId, productName, getType()));
    }


}
