package org.jetlinks.community.device.test.web;

import lombok.Getter;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.User;
import org.hswebframework.web.authorization.simple.SimpleDimension;
import org.hswebframework.web.authorization.simple.SimpleDimensionType;
import org.hswebframework.web.authorization.simple.SimplePermission;
import org.hswebframework.web.authorization.simple.SimpleUser;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Getter
public class TestAuthentication implements Authentication {

    private final User user;

    private final List<Dimension> dimensions = new ArrayList<>();
    private final List<Permission> permissions = new ArrayList<>();

    public TestAuthentication(String userId) {
        this.user = new SimpleUser(userId, userId, "test", "test", null);

    }

    public TestAuthentication addDimension(String type, String id) {
        dimensions.add(SimpleDimension.of(id, type, SimpleDimensionType.of(type), null));
        return this;
    }

    public TestAuthentication addPermission(String id, String... action) {
        permissions.add(SimplePermission.builder()
                                        .id(id)
                                        .name(id).actions(new HashSet<>(Arrays.asList(action)))
                                        .build());
        return this;
    }

    @Override
    public <T extends Serializable> Optional<T> getAttribute(String name) {
        return Optional.empty();
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return null;
    }

    @Override
    public Authentication merge(Authentication source) {
        return this;
    }

    @Override
    public Authentication copy(BiPredicate<Permission, String> permissionFilter,
                               Predicate<Dimension> dimension) {
        return this;
    }
}
