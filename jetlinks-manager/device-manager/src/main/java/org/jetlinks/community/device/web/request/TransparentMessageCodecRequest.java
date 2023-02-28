package org.jetlinks.community.device.web.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransparentMessageCodecRequest {
    @NotBlank
    private String provider;

    private Map<String,Object> configuration;
}
