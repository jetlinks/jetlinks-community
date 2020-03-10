package org.jetlinks.community.device.web.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.ProtocolSupport;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ProtocolInfo {
    private String id;

    private String name;

    public static ProtocolInfo of(ProtocolSupport support) {
        return of(support.getId(), support.getName());
    }
}