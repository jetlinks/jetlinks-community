package org.jetlinks.community.rule.engine.scene.internal.actions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.rule.engine.executor.device.DeviceDataTaskExecutorProvider;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProviders;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;
import org.jetlinks.community.rule.engine.scene.SceneAction;
import org.jetlinks.community.rule.engine.scene.SceneActionProvider;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author zhangji 2025/1/22
 * @since 2.3
 */
@Component
@AllArgsConstructor
public class DeviceDataActionProvider implements SceneActionProvider<DeviceDataActionProvider.DeviceDataAction> {
    public static final String PROVIDER = "device-data";

    private final ThingsRegistry registry;

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public DeviceDataAction newConfig() {
        return new DeviceDataAction();
    }

    @Override
    public List<String> parseColumns(DeviceDataAction config) {

        return Collections.emptyList();
    }

    @Override
    public Flux<Variable> createVariable(DeviceDataAction config) {
        return config
            .getSelector()
            .getDeviceMetadata(registry, config.getProductId())
            .map(config::createOutputType)
            .flatMapIterable(type -> {
                List<PropertyMetadata> props = type.getProperties();
                List<Variable> variables = new ArrayList<>(props.size());
                for (PropertyMetadata prop : props) {
                    variables.add(
                        SceneAction
                            .toVariable(prop.getId(),
                                        prop.getName(),
                                        prop.getValueType(),
                                        "message.scene.action.device-data." + prop.getId(),
                                        "设备[%s]信息",
                                        null)
                    );

                }
                return variables;
            });
    }

    @Override
    public void applyRuleNode(DeviceDataAction config, RuleNodeModel model) {

        model.setExecutor(DeviceDataTaskExecutorProvider.ID);
        if (DeviceSelectorProviders.isFixed(config.getSelector())) {
            config.setSelector(FastBeanCopier.copy(config.getSelector(), new DeviceSelectorSpec()));
        } else {
            config.setSelector(
                DeviceSelectorProviders.composite(
                    //先选择产品下的设备
                    DeviceSelectorProviders.product(config.getProductId()),
                    FastBeanCopier.copy(config.getSelector(), new DeviceSelectorSpec())
                ));
        }

        model.setConfiguration(config.toMap());
    }

    @Getter
    @Setter
    public static class DeviceDataAction extends DeviceDataTaskExecutorProvider.Config {
        private String productId;
    }
}
