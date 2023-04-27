package org.jetlinks.community.notify.event;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.utils.StringUtils;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.Provider;
import org.jetlinks.community.notify.template.Template;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@Getter
@Setter
@Builder
public class NotifierEvent {

    private boolean success;

    @Nullable
    private Throwable cause;

    @Nonnull
    private String notifierId;

    @Nonnull
    private NotifyType notifyType;

    @Nonnull
    private Provider provider;

    @Nullable
    private String templateId;

    @Nullable
    private Template template;

    @Nonnull
    private Map<String, Object> context;

    public SerializableNotifierEvent toSerializable() {
        return SerializableNotifierEvent.builder()
            .success(success)
            .notifierId(notifierId)
            .notifyType(notifyType.getId())
            .provider(provider.getId())
            .templateId(templateId)
            .template(template)
            .context(context)
            .cause(cause != null ? StringUtils.throwable2String(cause) : "")
            .errorType(cause != null ? cause.getClass().getName() : null)
            .build();
    }
}
