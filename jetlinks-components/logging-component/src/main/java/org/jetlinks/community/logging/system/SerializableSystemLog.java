package org.jetlinks.community.logging.system;

import lombok.*;

import java.io.Serializable;
import java.util.Map;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SerializableSystemLog implements Serializable {

    private String id;

    private String mavenModule;

    private String name;

    private String threadName;

    private String level;

    private String className;

    private String methodName;

    private int lineNumber;

    private String java;

    private String message;

    private String exceptionStack;

    private Long createTime;

    private String threadId;

    private Map<String, String> context;
}
