package org.jetlinks.community.resource.ui;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.resource.ClassPathJsonResourceProvider;

@Slf4j
public class UiMenuResourceProvider extends ClassPathJsonResourceProvider {
    public static final String TYPE = "ui-menus";


    public UiMenuResourceProvider() {
        super(TYPE, "classpath*:/ui/*/baseMenu.json");
    }
}