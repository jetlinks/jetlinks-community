/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.auth.configuration;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.User;
import org.hswebframework.web.authorization.events.AuthorizationInitializeEvent;
import org.hswebframework.web.authorization.simple.SimpleAuthentication;
import org.hswebframework.web.authorization.simple.SimplePermission;
import org.hswebframework.web.authorization.simple.SimpleUser;
import org.jetlinks.community.utils.ConverterUtils;
import org.springframework.context.event.EventListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 根据配置进行权限初始化
 */
@Slf4j
@AllArgsConstructor
public class AuthorizationPermissionInitializeService {

    private final AuthorizationProperties authorizationProperties;

    @EventListener
    public void handleAuthInitEvent(AuthorizationInitializeEvent event) {
        Authentication before = event.getAuthentication();
        List<Permission> defaultPermission = before
            .getDimensions()
            .stream()
            .flatMap(this::getPermission)
            .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(defaultPermission)) {
            SimpleAuthentication defaultAuth = new SimpleAuthentication();
            defaultAuth.setPermissions(defaultPermission);
            defaultAuth.merge(before);
            event.setAuthentication(defaultAuth);
        }
    }


    public Stream<Permission> getPermission(Dimension dimension) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        Optional
            .ofNullable(authorizationProperties.getDefaults().get(dimension.getType().getId()))
            .map(dimensionPermission -> {
                String dimensionKey = dimension.getId();
                if (dimension instanceof User) {
                    //用户类型根据username获取
                    dimensionKey = ((SimpleUser) dimension).getUsername();
                }
                return dimensionPermission.get(dimensionKey);
            })
            .orElse(Collections.emptyList())
            .forEach(text -> parse(text, map));

        return map
            .entrySet()
            .stream()
            .map(entry -> {
                String id = entry.getKey();
                Set<String> actions = entry
                    .getValue()
                    .stream()
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toSet());
                return SimplePermission
                    .builder()
                    .id(id)
                    .name(id)
                    .actions(actions)
                    .build();
            });
    }

    /**
     * 解析permissionText
     *
     * @param text: permissionId:action1,action2 例如device-instance:query,save
     */
    public static void parse(String text, MultiValueMap<String, String> map) {
        String[] split = text.split(":", 2);
        if (split.length == 2) {
            String permissionId = split[0];
            map.add(permissionId, null);

            if (StringUtils.isNotEmpty(split[1])) {
                ConverterUtils
                    .convertToList(split[1].split(","), String::valueOf)
                    .forEach(action -> map.add(permissionId, action));
            }
        }
    }
}