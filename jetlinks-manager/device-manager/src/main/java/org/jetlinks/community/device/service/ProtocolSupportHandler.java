package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.events.EntityBeforeDeleteEvent;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.device.entity.ProtocolSupportEntity;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 协议事件处理类.
 *
 * @author zhangji 2022/4/1
 */
@Component
@AllArgsConstructor
public class ProtocolSupportHandler {
    private final DataReferenceManager referenceManager;
    private       ProtocolSupportLoader  loader;
    private       ProtocolSupportManager supportManager;

    //禁止删除已有网关使用的协议
    @EventListener
    public void handleProtocolDelete(EntityBeforeDeleteEvent<ProtocolSupportEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(protocol -> referenceManager
                    .assertNotReferenced(DataReferenceManager.TYPE_PROTOCOL, protocol.getId()))
        );
    }

    @EventListener
    public void handleCreated(EntityCreatedEvent<ProtocolSupportEntity> event) {
        event.async(reloadProtocol(event.getEntity()));
    }

    @EventListener
    public void handleSaved(EntitySavedEvent<ProtocolSupportEntity> event) {
        event.async(reloadProtocol(event.getEntity()));
    }

    @EventListener
    public void handleModify(EntityModifyEvent<ProtocolSupportEntity> event) {
        event.async(reloadProtocol(event.getAfter()));
    }

    // 重新加载协议
    private Mono<Void> reloadProtocol(Collection<ProtocolSupportEntity> protocol) {
        return Flux
            .fromIterable(protocol)
            .filter(entity -> entity.getState() != null)
            .map(entity -> entity.getState() == 1 ? entity.toDeployDefinition() : entity.toUnDeployDefinition())
            .flatMap(def -> loader
                //加载一下检验是否正确，然后就卸载
                .load(def)
                .doOnNext(ProtocolSupport::dispose)
                .thenReturn(def))
            .as(LocaleUtils::transform)
            .onErrorMap(err -> {
                BusinessException e = new BusinessException("error.unable_to_load_protocol", 500, err.getLocalizedMessage());
                e.addSuppressed(err);
                return e;
            })
            .flatMap(supportManager::save)
            .then();
    }
}
