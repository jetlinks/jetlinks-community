package org.jetlinks.community.notify.template;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class TemplateProperties implements Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    private String type;

    private String provider;

    private String template;
}
