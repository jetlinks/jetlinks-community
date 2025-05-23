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
package org.jetlinks.community.authorize;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.authorization.*;
import org.hswebframework.web.authorization.simple.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.core.utils.SerializeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Setter
public class FastSerializableAuthentication extends SimpleAuthentication
    implements Externalizable, Jsonable {

    private static final Logger log = LoggerFactory.getLogger(FastSerializableAuthentication.class);

    static {
        SerializeUtils.registerSerializer(
            0x90,
            FastSerializableAuthentication.class,
            (ignore) -> new FastSerializableAuthentication());
    }

    public static void load() {

    }

    @SuppressWarnings("all")
    public static Authentication of(Object jsonOrObject, boolean share) {
        if (jsonOrObject == null) {
            return null;
        }
        //json
        if (jsonOrObject instanceof String) {
            return of((String) jsonOrObject, share);
        }
        // map
        if (jsonOrObject instanceof Map) {
            FastSerializableAuthentication fast = new FastSerializableAuthentication();
            fast.shared = share;
            fast.fromJson(new JSONObject((Map) jsonOrObject));
            return fast;
        }
        //auth
        if (jsonOrObject instanceof Authentication) {
            return of(((Authentication) jsonOrObject));
        }
        throw new UnsupportedOperationException();
    }

    public static Authentication of(String json, boolean share) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        FastSerializableAuthentication fast = new FastSerializableAuthentication();
        fast.shared = share;
        fast.fromJson(JSON.parseObject(json));
        return fast;
    }


    public static Authentication of(Authentication auth) {
        return of(auth, false);
    }

    public static Authentication of(Authentication auth, boolean simplify) {
        if (auth instanceof FastSerializableAuthentication) {
            ((FastSerializableAuthentication) auth).simplify = simplify;
            return auth;
        }
        FastSerializableAuthentication fast = new FastSerializableAuthentication();
        fast.setUser(auth.getUser());
        fast.setSimplify(simplify);
        fast.getPermissions().addAll(auth.getPermissions());
        fast.getDimensions().addAll(auth.getDimensions());
        fast.getAttributes().putAll(auth.getAttributes());
        return fast;
    }

    @Override
    protected FastSerializableAuthentication newInstance() {
        return new FastSerializableAuthentication();
    }

    /**
     * 是否简化,为true时,不序列化权限名称
     *
     * @see Permission#getName()
     */
    private boolean simplify = false;

    private transient boolean shared;

    public void makeShared() {
        shared = true;
        List<Dimension> dimensions = getDimensions()
            .stream()
            .map(RecyclerUtils::intern)
            .collect(Collectors.toList());
        setDimensions(dimensions);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        String userId = null;
        try {
            out.writeByte(0x01);
            //是否简化模式
            out.writeBoolean(simplify);

            //user
            User user = getUser();
            out.writeUTF(user.getId() == null ? "" : (userId = user.getId()));
            SerializeUtils.writeNullableUTF(user.getName(), out);
            out.writeUTF(user.getUsername() == null ? "" : user.getUsername());
            SerializeUtils.writeNullableUTF(user.getUserType(), out);
            SerializeUtils.writeKeyValue(user.getOptions(), out);

            //permission
            {
                List<Permission> permissions = getPermissions();
                if (permissions == null) {
                    permissions = Collections.emptyList();
                }
                out.writeInt(permissions.size());
                for (Permission permission : permissions) {
                    write(permission, out);
                }
            }
            //dimension
            {
                List<Dimension> dimensions = getDimensions();
                if (dimensions == null) {
                    dimensions = Collections.emptyList();
                }
                out.writeInt(dimensions.size());
                for (Dimension permission : dimensions) {
                    write(permission, out);
                }
            }

            SerializeUtils.writeKeyValue(getAttributes(), out);

        } catch (Throwable e) {
            log.warn("write FastSerializableAuthentication [{}] error", userId, e);
            throw e;
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        byte version = in.readByte();
        simplify = in.readBoolean();

        //user
        SimpleUser user = new SimpleUser();
        user.setId(in.readUTF());
        user.setName(SerializeUtils.readNullableUTF(in));
        user.setUsername(in.readUTF());
        user.setUserType(SerializeUtils.readNullableUTF(in));
        user.setOptions(SerializeUtils.readMap(in, Maps::newHashMapWithExpectedSize));

        setUser0(user);

        //permission
        {
            int size = in.readInt();
            List<Permission> permissions = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Permission permission = readPermission(in);
                permissions.add(permission);
            }
            setPermissions(permissions);
        }
        //dimension
        {
            int size = in.readInt();
            Set<Dimension> dimensions = new HashSet<>(size);
            for (int i = 0; i < size; i++) {
                Dimension dimension = readDimension(in);
                dimensions.add(dimension);
            }
            setDimensions(dimensions);
        }

        setAttributes(SerializeUtils.readMap(in, Maps::newHashMapWithExpectedSize));
    }

    @SneakyThrows
    private Dimension readDimension(ObjectInput in) {
        SimpleDimension dimension = new SimpleDimension();
        dimension.setId(in.readUTF());
        dimension.setName(in.readUTF());

        dimension.setOptions(SerializeUtils.readMap(
            in,
            k -> RecyclerUtils.intern(String.valueOf(k)),
            Function.identity(),
            Maps::newHashMapWithExpectedSize));

        boolean known = in.readBoolean();
        if (known) {
            KnownDimension knownDimension = KnownDimension.ALL[in.readByte()];
            dimension.setType(knownDimension.type);
        } else {
            SimpleDimensionType type = new SimpleDimensionType();
            type.setId(in.readUTF());
            type.setName(in.readUTF());
            dimension.setType(RecyclerUtils.intern(type));
        }
        return dimension;
    }


    @SneakyThrows
    private void write(Dimension dimension, ObjectOutput out) {
        out.writeUTF(dimension.getId());
        out.writeUTF(dimension.getName() == null ? "" : dimension.getName());

        SerializeUtils.writeKeyValue(dimension.getOptions(), out);

        KnownDimension knownDimension = KnownDimension.MAPPING.get(dimension.getType().getId());

        out.writeBoolean(knownDimension != null);
        if (knownDimension != null) {
            out.writeByte(knownDimension.ordinal());
        } else {
            out.writeUTF(dimension.getType().getId());
            out.writeUTF(dimension.getType().getName());
        }

    }


    @SneakyThrows
    private Permission readPermission(ObjectInput in) {
        SimplePermission permission = new SimplePermission();
        permission.setId(in.readUTF());
        if (!simplify) {
            permission.setName(in.readUTF());
        } else {
            permission.setName(permission.getId());
        }
        permission.setOptions(SerializeUtils.readMap(in, Maps::newHashMapWithExpectedSize));

        int actionSize = in.readUnsignedShort();
        Set<String> actions = Sets.newHashSetWithExpectedSize(actionSize);
        for (int i = 0; i < actionSize; i++) {
            if (in.readBoolean()) {
                actions.add(KnownAction.ALL[in.readByte()].action);
            } else {
                actions.add(in.readUTF());
            }
        }
        permission.setActions(actions);
        return permission;
    }

    @SneakyThrows
    private void write(Permission permission, ObjectOutput out) {
        out.writeUTF(permission.getId());
        if (!simplify) {
            out.writeUTF(permission.getName() == null ? "" : permission.getName());
        }

        SerializeUtils.writeKeyValue(permission.getOptions(), out);
        Set<String> actions = permission.getActions();
        out.writeShort(actions.size());

        for (String action : actions) {
            KnownAction knownAction = KnownAction.ACTION_MAP.get(action);
            out.writeBoolean(knownAction != null);
            if (null != knownAction) {
                out.writeByte(knownAction.ordinal());
            } else {
                out.writeUTF(action);
            }
        }
    }

    @AllArgsConstructor
    enum KnownDimension {
        user(DefaultDimensionType.user),
        role(DefaultDimensionType.role),
        org(OrgDimensionType.org),
        parentOrg(OrgDimensionType.parentOrg);
        private final DimensionType type;
        static final KnownDimension[] ALL = values();
        static final Map<Object, KnownDimension> MAPPING = new HashMap<>();

        static {
            for (KnownDimension value : ALL) {
                MAPPING.put(value, value);
                MAPPING.put(value.ordinal(), value);
                MAPPING.put(value.name(), value);
            }
        }
    }

    enum KnownAction {
        query,
        get,
        update,
        save,
        delete,
        export,
        _import(Permission.ACTION_IMPORT),
        enable,
        disable;
        static final KnownAction[] ALL = values();
        static final Map<Object, KnownAction> ACTION_MAP = new HashMap<>();

        static {
            for (KnownAction value : ALL) {
                ACTION_MAP.put(value, value);
                ACTION_MAP.put(value.ordinal(), value);
                ACTION_MAP.put(value.action, value);
            }
        }

        private final String action;

        KnownAction() {
            this.action = name();
        }

        KnownAction(String action) {
            this.action = action;
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("user", SerializeUtils.convertToSafelySerializable(getUser()));
        obj.put("permissions", SerializeUtils.convertToSafelySerializable(getPermissions()));
        //忽略user
        obj.put("dimensions", SerializeUtils.convertToSafelySerializable(
            Collections2.filter(getDimensions(), i -> !(i instanceof User))
        ));
        obj.put("attributes", new HashMap<>(getAttributes()));

        return obj;
    }

    @Override
    public void fromJson(JSONObject json) {
        JSONObject user = json.getJSONObject("user");
        if (user != null) {
            setUser(user.toJavaObject(SimpleUser.class));
        }
        JSONArray permissions = json.getJSONArray("permissions");
        if (permissions != null) {
            for (int i = 0, size = permissions.size(); i < size; i++) {
                JSONObject permission = permissions.getJSONObject(i);
                //不再支持
                permission.remove("dataAccesses");

                Object actions = permission.remove("actions");
                SimplePermission perm = permission.toJavaObject(SimplePermission.class);

                if (actions instanceof Collection) {
                    @SuppressWarnings("all")
                    Collection<Object> _actions = (Collection<Object>) actions;
                    Set<String> acts = Sets.newHashSetWithExpectedSize(_actions.size());
                    for (Object action : _actions) {
                        KnownAction act = KnownAction.ACTION_MAP.get(action);
                        if (act == null) {
                            acts.add(String.valueOf(action));
                        } else {
                            acts.add(act.action);
                        }
                    }
                    perm.setActions(acts);
                }

                getPermissions().add(shared ? RecyclerUtils.intern(perm) : perm);
            }
        }
        JSONArray dimensions = json.getJSONArray("dimensions");

        if (dimensions != null) {
            for (int i = 0, size = dimensions.size(); i < size; i++) {
                JSONObject dimension = dimensions.getJSONObject(i);
                Object type = dimension.remove("type");
                if (type == null) {
                    continue;
                }
                SimpleDimension simpleDimension = dimension.toJavaObject(SimpleDimension.class);
                if (type instanceof DimensionType) {
                    simpleDimension.setType((DimensionType) type);
                } else {
                    KnownDimension knownDimension = KnownDimension.MAPPING.get(type);
                    if (knownDimension != null) {
                        simpleDimension.setType(knownDimension.type);
                    } else {
                        SimpleDimensionType dimensionType;
                        if (type instanceof String) {
                            dimensionType = SimpleDimensionType.of(String.valueOf(type));
                        } else {
                            dimensionType = FastBeanCopier.copy(type, new SimpleDimensionType());
                        }
                        if (StringUtils.isNoneEmpty(dimensionType.getId())) {
                            simpleDimension.setType(shared ? RecyclerUtils.intern(dimensionType) : dimensionType);
                        }
                    }
                }
                getDimensions().add(shared ? RecyclerUtils.intern(simpleDimension) : simpleDimension);
            }
        }
        JSONObject attr = json.getJSONObject("attributes");
        if (attr != null) {
            getAttributes().putAll(Maps.transformValues(attr, Serializable.class::cast));
        }

    }
}
