package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class PermissionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "权限ID")
    private String permission;

    @Schema(description = "权限操作")
    private Set<String> actions;

}
