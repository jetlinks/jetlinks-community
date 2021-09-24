package org.jetlinks.community.configure.r2dbc;

import io.r2dbc.spi.ValidationDepth;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.r2dbc.pool")
public class R2dbcPoolProperties {

    /**
     * Maximum lifetime of a connection in the pool. By default, connections have an
     * infinite lifetime.
     */
    private Duration maxLifeTime = Duration.ofMinutes(10);

    /**
     * Maximum time to acquire a connection from the pool. By default, wait
     * indefinitely.
     */
    private Duration maxAcquireTime;

    private int acquireRetry = 3;
    /**
     * Maximum time to wait to create a new connection. By default, wait indefinitely.
     */
    private Duration maxCreateConnectionTime;

    /**
     * Validation query.
     */
    private String validationQuery;

    /**
     * Validation depth.
     */
    private ValidationDepth validationDepth = ValidationDepth.LOCAL;

}
