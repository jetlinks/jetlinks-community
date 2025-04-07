package org.jetlinks.community;

public interface OperationType {
    String getId();

    String getName();

    static OperationType of(String id,String name){
        return new DynamicOperationType(id,name);
    }
}
