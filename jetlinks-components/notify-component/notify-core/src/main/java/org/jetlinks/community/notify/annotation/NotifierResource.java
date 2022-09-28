package org.jetlinks.community.notify.annotation;

import org.hswebframework.web.authorization.annotation.Resource;

import java.lang.annotation.*;

@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Resource(id = "notifier", name = "通知管理")
public @interface NotifierResource {

}
