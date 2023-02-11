package org.jetlinks.community.relation.impl;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.things.relation.ObjectType;
import org.jetlinks.core.things.relation.Relation;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class SimpleObjectType implements ObjectType, Externalizable {
    private String id;
    private String name;
    private String description;
    private Map<String, Object> expands;
    private Map<String, List<Relation>> relations;

    private List<ObjectType> relatedTypes;
    private List<PropertyMetadata> properties;

    public SimpleObjectType() {

    }

    public SimpleObjectType(ObjectType type) {
        this(type.getId(), type.getName(), type.getDescription());
        this.expands = type.getExpands();
        this.relations = type.getRelations();
        this.properties = type.getProperties();
        this.relatedTypes = type.getRelatedTypes();
    }


    public SimpleObjectType(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }


    public static SimpleObjectType of(ObjectType type) {
        if (type instanceof SimpleObjectType) {
            return (SimpleObjectType) type;
        }
        return new SimpleObjectType(type);
    }

    @Override
    public List<Relation> getRelations(String type) {
        if (relations == null) {
            return Collections.emptyList();
        }
        return relations.getOrDefault(type, Collections.emptyList());
    }

    public SimpleObjectType withExpands(Map<String, Object> expands) {
        if (this.expands == null) {
            this.expands = new HashMap<>();
        }
        this.expands.putAll(expands);
        return this;
    }

    public SimpleObjectType withExpand(String key, String value) {
        if (this.expands == null) {
            this.expands = new HashMap<>();
        }
        this.expands.put(key, value);
        return this;
    }

    public SimpleObjectType withRelation(String type, List<? extends Relation> relation) {
        if (relations == null) {
            relations = new HashMap<>();
        }
        relations.computeIfAbsent(type, (ignore) -> new ArrayList<>())
                 .addAll(relation);
        return this;
    }

    public SimpleObjectType withProperty(String id, String name, DataType type) {
        return withProperty(SimplePropertyMetadata.of(id, name, type));
    }

    public Map<String, List<Relation>> getRelations() {
        return Collections.unmodifiableMap(relations);
    }

    public SimpleObjectType withProperty(PropertyMetadata property) {
        if (properties == null) {
            properties = new ArrayList<>();
        }
        properties.add(property);
        return this;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        SerializeUtils.writeObject(id, out);
        SerializeUtils.writeObject(name, out);
        SerializeUtils.writeObject(description, out);

        SerializeUtils.writeObject(expands, out);
        SerializeUtils.writeObject(relations, out);

        //复制为SimpleObjectType便于序列化
        List<ObjectType> relatedTypes = this.relatedTypes;
        if (relatedTypes != null) {
            relatedTypes = relatedTypes
                .stream()
                .map(SimpleObjectType::of)
                .collect(Collectors.toList());
        }

        SerializeUtils.writeObject(relatedTypes, out);
        SerializeUtils.writeObject(properties, out);
    }

    @SuppressWarnings("all")
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = (String) SerializeUtils.readObject(in);
        name = (String) SerializeUtils.readObject(in);
        description = (String) SerializeUtils.readObject(in);

        expands = (Map) SerializeUtils.readObject(in);
        relations = (Map) SerializeUtils.readObject(in);
        relatedTypes = (List) SerializeUtils.readObject(in);
        properties = (List) SerializeUtils.readObject(in);
    }
}
