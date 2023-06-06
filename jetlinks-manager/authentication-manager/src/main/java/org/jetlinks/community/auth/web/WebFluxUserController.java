package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.User;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.system.authorization.api.PasswordValidator;
import org.hswebframework.web.system.authorization.api.UsernameValidator;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultReactiveUserService;
import org.jetlinks.community.auth.enums.DefaultUserEntityType;
import org.jetlinks.community.web.response.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

public class WebFluxUserController extends org.hswebframework.web.system.authorization.defaults.webflux.WebFluxUserController {

    @Autowired
    private DefaultReactiveUserService reactiveUserService;

    @Autowired(required = false)
    private PasswordValidator passwordValidator = (password) -> {
    };

    @Autowired(required = false)
    private UsernameValidator usernameValidator = (username) -> {
        if (StringUtils.isEmpty(username)) {
            throw new ValidationException("error.user_cannot_be_empty");
        }
    };


    @PostMapping("/{id}/password/_reset")
    @SaveAction
    @Operation(summary = "重置密码")
    public Mono<Boolean> resetPassword(@PathVariable String id,
                                       @RequestBody String password) {
        return Mono.defer(() -> {
            passwordValidator.validate(password);
            UserEntity user = new UserEntity();
            user.setPassword(password);
            user.setId(id);
            return reactiveUserService.saveUser(Mono.just(user));
        });
    }

    @PostMapping("/username/_validate")
    @Authorize(merge = false)
    @Operation(summary = "用户名验证")
    public Mono<ValidationResult> usernameValidate(@RequestBody(required = false) String username) {

        return LocaleUtils
            .currentReactive()
            .flatMap(locale -> {
                usernameValidator.validate(username);
                return reactiveUserService
                    .findByUsername(username)
                    .map(i -> ValidationResult.error(LocaleUtils.resolveMessage(
                        "error.user_already_exist",
                        locale,
                        "用户" + username + "已存在", username))
                    );
            })
            .defaultIfEmpty(ValidationResult.success())
            .onErrorResume(ValidationException.class,
                           e -> e.getLocalizedMessageReactive().map(ValidationResult::error));
    }


    @PostMapping("/password/_validate")
    @Authorize(merge = false)
    @Operation(summary = "密码验证")
    public Mono<ValidationResult> passwordValidate(@RequestBody(required = false) String password) {
        return Mono
            .fromSupplier(() -> {
                passwordValidator.validate(password);
                return ValidationResult.success();
            })
            .onErrorResume(ValidationException.class,
                           e -> e.getLocalizedMessageReactive().map(ValidationResult::error));
    }

    @PostMapping("/me/password/_validate")
    @SaveAction
    @Operation(summary = "校验当前用户的密码")
    @Authorize(merge = false)
    public Mono<ValidationResult> loginUserPasswordValidate(@RequestBody(required = false) String password) {
        return LocaleUtils
            .currentReactive()
            .flatMap(locale -> {
                // 不校验密码长度与强度，管理员初始密码可能无法通过校验
//                passwordValidator.validate(password);

                return Authentication
                    .currentReactive()
                    .map(Authentication::getUser)
                    .map(User::getUsername)
                    .flatMap(username -> reactiveUserService
                        .findByUsernameAndPassword(username, password)
                        .flatMap(ignore -> Mono.just(ValidationResult.success())))
                    .switchIfEmpty(Mono.just(ValidationResult
                                                 .error(LocaleUtils
                                                            .resolveMessage("error.password_not_correct", locale, "密码错误"))));
            })
            .onErrorResume(ValidationException.class,
                           e -> e.getLocalizedMessageReactive().map(ValidationResult::error));
    }

    @Override
    public Mono<Boolean> saveUser(@RequestBody Mono<UserEntity> userMono) {
        return userMono
            .flatMap(user -> {
                Mono<Void> before;
                boolean isNew = StringUtils.isEmpty(user.getId());
                if (!isNew) {
                    //如果不是新创建用户,则判断是否有权限修改
                    before = assertUserPermission(user.getId());
                } else {
                    before = Mono.fromRunnable(() -> {
                        // 新建用户时，默认设置类型为普通用户
                        if (StringUtils.isEmpty(user.getType()) && !user.getUsername().equals("admin")) {
                            user.setType(DefaultUserEntityType.USER.getId());
                        }
                    });
                }
                return before
                    .then(super.saveUser(Mono.just(user)))
                    .thenReturn(true);
            });
    }

    @Override
    public Mono<Boolean> deleteUser(@PathVariable String id) {
        return assertUserPermission(id)
            .then(super.deleteUser(id))
            ;
    }

    @Override
    public Mono<Integer> changeState(@PathVariable @Parameter(description = "用户ID") String id,
                                     @PathVariable @Parameter(description = "状态,0禁用,1启用") Byte state) {
        return assertUserPermission(id)
            .then(
                super.changeState(id, state)
            );
    }

    @Override
    public Mono<UserEntity> getById(@PathVariable String id) {
        return assertUserPermission(id)
            .then(super.getById(id));
    }

    private Mono<Void> assertUserPermission(String userId) {
        return Mono.empty();
    }
}
