package org.jetlinks.community.rule.engine.executor.device;

import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.community.relation.utils.VariableSource;
import org.jetlinks.reactor.ql.utils.CastUtils;

import java.util.*;
import java.util.stream.Collectors;

public class DeviceSelectorProviders {

    private final static Map<String, DeviceSelectorProvider> providers = new LinkedHashMap<>();

    static {

        register(SimpleDeviceSelectorProvider
                     .of(
                         "all", "全部设备",
                         (args, query) -> query));

        register(SimpleDeviceSelectorProvider
                     .of(
                         "fixed", "固定设备",
                         (args, query) -> query.in("id", args)));

        register(SimpleDeviceSelectorProvider
                     .of("state", "按状态",
                         (args, query) -> query.in("state", args)));

        register(SimpleDeviceSelectorProvider
                     .of("product", "按产品",
                         (args, query) -> query.in("productId", args)));

        register(SimpleDeviceSelectorProvider
                     .of("tag", "按标签",
                         (args, query) -> {
                             if (args.size() == 1) {
                                 return query.accept("id",
                                                     "dev-tag",
                                                     args.get(0));
                             }
                             Map<Object, Object> tagsMap = Maps.newLinkedHashMapWithExpectedSize(args.size());
                             List<Object> temp = new ArrayList<>(args.size());
                             for (Object val : args) {
                                 // {key:value}
                                 if (val instanceof Map) {
                                     tagsMap.putAll(((Map) val));
                                 }
                                 // key:value
                                 else if (val instanceof String) {
                                     String[] arr = String.valueOf(val).split(":");
                                     if (arr.length == 2) {
                                         tagsMap.put(arr[0], arr[1]);
                                     }
                                 } else {
                                     temp.add(val);
                                 }
                             }
                             if (CollectionUtils.isNotEmpty(temp)) {
                                 tagsMap.putAll(CastUtils.castMap(temp));
                             }
                             return query.accept("id",
                                                 "dev-tag",
                                                 tagsMap);
                         }));

        register(new CompositeDeviceSelectorProvider());

    }

    public static DeviceSelectorSpec fixed(Object value) {
        DeviceSelectorSpec spec = new DeviceSelectorSpec();
        spec.setSelector("fixed");
        spec.setSource(VariableSource.Source.fixed);
        spec.setValue(value);
        return spec;
    }

    public static DeviceSelectorSpec product(String productId) {
        DeviceSelectorSpec spec = new DeviceSelectorSpec();
        spec.setSelector("product");
        spec.setSource(VariableSource.Source.fixed);
        spec.setValue(productId);
        return spec;
    }

    public static DeviceSelectorSpec composite(Collection<DeviceSelectorSpec> specs) {
        DeviceSelectorSpec composite = new DeviceSelectorSpec();
        composite.setSource(VariableSource.Source.fixed);
        composite.setSelector(CompositeDeviceSelectorProvider.PROVIDER);
        composite.setSelectorValues(specs
                                        .stream()
                                        .map(spec -> SelectorValue.of(spec, null))
                                        .collect(Collectors.toList()));
        return composite;

    }

    public static DeviceSelectorSpec composite(DeviceSelectorSpec... specs) {
        return composite(Arrays.asList(specs));
    }

    public static void register(DeviceSelectorProvider provider) {
        providers.put(provider.getProvider(), provider);
    }

    public static Optional<DeviceSelectorProvider> getProvider(String provider) {
        return Optional.ofNullable(providers.get(provider));
    }

    public static DeviceSelectorProvider getProviderNow(String provider) {
        return getProvider(provider)
            .orElseThrow(() -> new UnsupportedOperationException("unsupported device selector provider:" + provider));
    }

    public static List<DeviceSelectorProvider> allProvider() {
        return new ArrayList<>(providers.values());
    }

}
