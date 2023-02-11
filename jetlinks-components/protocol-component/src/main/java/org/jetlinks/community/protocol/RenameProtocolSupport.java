package org.jetlinks.community.protocol;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.*;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.message.codec.TraceDeviceMessageCodec;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.route.Route;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.DeviceGatewayContext;
import org.jetlinks.community.topic.Topics;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * 重命名协议，将协议包里的协议使用进行重命名
 *
 * @author zhouhao
 * @since 1.2
 */
@AllArgsConstructor
@Generated
public class RenameProtocolSupport implements ProtocolSupport {

    public static final JetLinksDeviceMetadataCodec metadataCodec = new JetLinksDeviceMetadataCodec();
    @Getter
    private final String id;

    @Getter
    private final String name;

    @Getter
    private final String description;

    private final ProtocolSupport target;

    private final EventBus eventBus;

    @Override
    public Flux<? extends Transport> getSupportedTransport() {
        return target.getSupportedTransport();
    }

    @Nonnull
    @Override
    public Mono<? extends DeviceMessageCodec> getMessageCodec(Transport transport) {
        return target
            .getMessageCodec(transport)
            .map(codec-> new TraceDeviceMessageCodec(id,codec));
    }

    @Override
    public Mono<DeviceMessageSenderInterceptor> getSenderInterceptor() {
        return target.getSenderInterceptor();
    }

    @Nonnull
    @Override
    @SuppressWarnings("all")
    public DeviceMetadataCodec getMetadataCodec() {
        return target.getMetadataCodec() == null ? metadataCodec : target.getMetadataCodec();
    }

    @Nonnull
    @Override
    public Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request,
                                                     @Nonnull DeviceOperator deviceOperation) {
        return target.authenticate(request, deviceOperation);
    }

    @Nonnull
    @Override
    public Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request,
                                                     @Nonnull DeviceRegistry registry) {
        return target.authenticate(request, registry);
    }

    @Override
    public Mono<DeviceMetadata> getDefaultMetadata(Transport transport) {
        return target.getDefaultMetadata(transport);
    }

    @Override
    public Flux<ConfigMetadata> getMetadataExpandsConfig(Transport transport,
                                                         DeviceMetadataType metadataType,
                                                         String metadataId,
                                                         String dataTypeId) {
        return target.getMetadataExpandsConfig(transport, metadataType, metadataId, dataTypeId);
    }

    @Override
    public Flux<DeviceMetadataCodec> getMetadataCodecs() {
        return target.getMetadataCodecs();
    }

    @Override
    public Mono<ConfigMetadata> getInitConfigMetadata() {
        return target.getInitConfigMetadata();
    }

    @Nonnull
    @Override
    public Mono<DeviceStateChecker> getStateChecker() {
        return target.getStateChecker();
    }

    @Override
    public Mono<ConfigMetadata> getConfigMetadata(Transport transport) {
        return target.getConfigMetadata(transport);
    }

    @Override
    public void init(Map<String, Object> configuration) {
        target.init(configuration);
    }

    @Override
    public void dispose() {
        target.dispose();
    }

    @Override
    public boolean isDisposed() {
        return target.isDisposed();
    }

    @Override
    public Mono<Void> onDeviceUnRegister(DeviceOperator operator) {
        return target.onDeviceUnRegister(operator)
                     .then(eventBus
                               .publish(Topics.deviceUnRegisterEvent(operator.getDeviceId()), operator.getDeviceId())
                               .then());
    }

    @Override
    public Mono<Void> onDeviceRegister(DeviceOperator operator) {
        return target.onDeviceRegister(operator)
                     .then(eventBus
                               .publish(Topics.deviceRegisterEvent(operator.getDeviceId()), operator.getDeviceId())
                               .then());
    }

    @Override
    public Mono<Void> onProductRegister(DeviceProductOperator operator) {
        return target.onProductRegister(operator)
                     .then(eventBus
                               .publish(Topics.productRegisterEvent(operator.getId()), operator.getId())
                               .then());
    }

    @Override
    public Mono<Void> onProductUnRegister(DeviceProductOperator operator) {
        return target.onProductUnRegister(operator)
                     .then(eventBus
                               .publish(Topics.productUnRegisterEvent(operator.getId()), operator.getId())
                               .then());
    }

    @Override
    public Mono<Void> onDeviceMetadataChanged(DeviceOperator operator) {
        return target.onDeviceMetadataChanged(operator)
                     .then(eventBus
                               .publish(Topics.deviceMetadataChangedEvent(operator.getDeviceId()), operator.getDeviceId())
                               .then());
    }

    @Override
    public Mono<Void> onProductMetadataChanged(DeviceProductOperator operator) {
        return target.onProductMetadataChanged(operator)
                     .then(eventBus
                               .publish(Topics.productMetadataChangedEvent(operator.getId()), operator.getId())
                               .then());
    }

    @Override
    public Mono<Void> onChildBind(DeviceOperator gateway, Flux<DeviceOperator> child) {
        return target.onChildBind(gateway, child);
    }

    @Override
    public Mono<Void> onChildUnbind(DeviceOperator gateway, Flux<DeviceOperator> child) {
        return target.onChildUnbind(gateway, child);
    }

    @Override
    public Mono<Void> onClientConnect(Transport transport, ClientConnection connection, DeviceGatewayContext context) {
        return target.onClientConnect(transport, connection, context);
    }

    @Override
    public Flux<Feature> getFeatures(Transport transport) {
        return target.getFeatures(transport);
    }

    @Override
    public Mono<DeviceInfo> doBeforeDeviceCreate(Transport transport, DeviceInfo deviceInfo) {
        return target.doBeforeDeviceCreate(transport, deviceInfo);
    }

    @Override
    public int getOrder() {
        return target.getOrder();
    }

    @Override
    public int compareTo(ProtocolSupport o) {
        return target.compareTo(o);
    }

    @Override
    public Flux<Route> getRoutes(Transport transport) {
        return target.getRoutes(transport);
    }

    @Override
    public String getDocument(Transport transport) {
        return target.getDocument(transport);
    }

    @Override
    public boolean isEmbedded() {
        return target.isEmbedded();
    }
}
