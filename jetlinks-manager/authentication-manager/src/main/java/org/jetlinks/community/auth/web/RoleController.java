package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.auth.entity.RoleEntity;
import org.jetlinks.community.auth.service.RoleService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/role")
@Resource(id = "role", name = "角色管理")
@AllArgsConstructor
@Getter
@Tag(name = "角色管理")
public class RoleController implements ReactiveServiceCrudController<RoleEntity, String> {

    private final RoleService service;

    @PostMapping("/{roleId}/users/_bind")
    @Operation(summary = "绑定用户")
    @SaveAction
    public Mono<Void> bindUser(@PathVariable String roleId,
                               @RequestBody Mono<List<String>> userId) {
        return userId
            .flatMap(list -> service.bindUser(list, Collections.singleton(roleId), false));
    }

    @PostMapping("/{roleId}/users/_unbind")
    @Operation(summary = "解绑用户")
    @SaveAction
    public Mono<Void> unbindUser(@PathVariable String roleId,
                                 @RequestBody Mono<List<String>> userId) {
        return userId
            .flatMap(list -> service.unbindUser(list, Collections.singleton(roleId)));
    }
}
