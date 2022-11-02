package org.jetlinks.community.auth;

import org.hswebframework.web.authorization.simple.DefaultAuthorizationAutoConfiguration;
import org.hswebframework.web.starter.jackson.CustomCodecsAutoConfiguration;
import org.hswebframework.web.system.authorization.defaults.configuration.AuthorizationServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration({
    AuthorizationServiceAutoConfiguration.class,
    CodecsAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    CustomCodecsAutoConfiguration.class,
    DefaultAuthorizationAutoConfiguration.class
})
public class AuthenticationTestConfiguration {


}
