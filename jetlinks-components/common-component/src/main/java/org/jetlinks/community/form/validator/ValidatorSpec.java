package org.jetlinks.community.form.validator;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ValidatorSpec {

    private String provider;

    private Map<String,Object> configuration;
}
