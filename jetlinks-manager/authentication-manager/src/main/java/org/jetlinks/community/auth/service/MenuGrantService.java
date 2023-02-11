package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.auth.entity.MenuBindEntity;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.service.request.MenuGrantRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class MenuGrantService {

    private final AuthorizationSettingDetailService settingService;
    private final ReactiveRepository<MenuBindEntity, String> bindRepository;
    private final ReactiveRepository<MenuEntity, String> menuRepository;


    /**
     * 对菜单进行授权
     *
     * @param request 授权请求
     * @return void
     */
    @Transactional
    public Mono<Void> grant(MenuGrantRequest request) {
        return Flux
            .concat(
                //先删除原已保存的菜单信息
                bindRepository
                    .createDelete()
                    .where(MenuBindEntity::getTargetType, request.getTargetType())
                    .and(MenuBindEntity::getTargetId, request.getTargetId())
                    .execute(),
                //保存菜单信息
                bindRepository.save(request.toBindEntities()),
                settingService.clearPermissionUserAuth(request.getTargetType(), request.getTargetId())
            )
//                //保存权限信息
//                Mono
//                    .zip(Mono.just(request),
//                         menuRepository
//                             .createQuery()
//                             .where(MenuEntity::getStatus, 1)
//                             .fetch()
//                             .collectList(),
//                         MenuGrantRequest::toAuthorizationSettingDetail
//                    )
//                    //保存后端权限信息
//                    .flatMap(setting -> settingService.saveDetail(null, Flux.just(setting))))
            .then()
            ;
    }
}
