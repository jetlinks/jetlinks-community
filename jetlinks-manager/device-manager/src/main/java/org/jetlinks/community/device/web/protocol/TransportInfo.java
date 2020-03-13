package org.jetlinks.community.device.web.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.message.codec.Transport;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class TransportInfo {
    private String id;

    private String name;

    public  static TransportInfo of(Transport support) {
        return of(support.getId(), support.getName());
    }
}