package org.jetlinks.community.plugin.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.message.codec.Transport;

@AllArgsConstructor
@Getter
public enum PluginTransport implements Transport {

    plugin("插件");

    private final String name;

    @Override
    public String getId() {
        return name();
    }
}
