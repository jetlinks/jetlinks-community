package org.jetlinks.community.notify.event;

import lombok.*;
import org.jetlinks.community.notify.template.Template;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerializableNotifierEvent implements Serializable {

    private String id;

    private boolean success;

    @Nullable
    private String errorType;

    @Nullable
    private String cause;

    @Nonnull
    private String notifierId;

    @Nonnull
    private String notifyType;

    @Nonnull
    private String provider;

    @Nullable
    private String templateId;

    @Nullable
    private Template template;

    @Nonnull
    private Map<String,Object> context;

    private long sendTime;
}
