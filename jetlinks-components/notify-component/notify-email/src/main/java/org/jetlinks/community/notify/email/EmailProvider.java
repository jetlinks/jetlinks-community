package org.jetlinks.community.notify.email;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.jetlinks.community.notify.Provider;

@Getter
@AllArgsConstructor
@Generated
public enum EmailProvider implements Provider {

    embedded("默认")
    ;

    private String name;

    @Override
    public String getId() {
        return name();
    }
}
