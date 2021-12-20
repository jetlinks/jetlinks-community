package org.jetlinks.community.device.configuration;

import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import org.jetlinks.supports.official.JetLinksDeviceMetadata;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.jetlinks.supports.test.MockProtocolSupport;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

public class TestMockSupport extends MockProtocolSupport {
    @Override
    public Mono<DeviceMetadata> getDefaultMetadata(Transport transport) {
        return Mono.just(new JetLinksDeviceMetadata("test","test"));
    }

    @Nonnull
    @Override
    public DeviceMetadataCodec getMetadataCodec() {
        return JetLinksDeviceMetadataCodec.getInstance();
    }
}
