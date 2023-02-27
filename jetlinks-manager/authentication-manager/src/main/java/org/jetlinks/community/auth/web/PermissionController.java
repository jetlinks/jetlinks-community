package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.system.authorization.api.entity.PermissionEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultPermissionService;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.community.web.response.ValidationResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 权限管理.
 *
 * @author zhangji 2022/12/23
 */
@RestController
@RequestMapping("/permission")
@Authorize
@Resource(id = "permission", name = "权限管理", group = "system")
@Tag(name = "权限管理")
@AllArgsConstructor
public class PermissionController {

    private final DefaultPermissionService permissionService;

    @GetMapping("/id/_validate")
    @QueryAction
    @Operation(summary = "验证权限ID是否合法")
    public Mono<ValidationResult> permissionIdValidate2(@RequestParam @Parameter(description = "权限ID") String id) {
        return LocaleUtils.currentReactive()
            .flatMap(locale -> {
                PermissionEntity entity = new PermissionEntity();
                entity.setId(id);
                entity.tryValidate("id", CreateGroup.class);

                return permissionService
                    .findById(id)
                    .map(permission -> ValidationResult
                        .error(LocaleUtils.resolveMessage("error.id_already_exists", locale)));
            })
            .defaultIfEmpty(ValidationResult.success())
            .onErrorResume(ValidationException.class, e -> Mono.just(e.getI18nCode())
                .map(ValidationResult::error));
    }
}
