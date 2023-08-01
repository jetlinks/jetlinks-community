package org.jetlinks.community.rule.engine.scene.internal.actions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DelayAction implements Serializable {
    @Schema(description = "延迟时间")
    private int time;

    @Schema(description = "时间单位")
    private DelayUnit unit;

    @Getter
    @AllArgsConstructor
    public enum DelayUnit {
        seconds(ChronoUnit.SECONDS),
        minutes(ChronoUnit.MINUTES),
        hours(ChronoUnit.HOURS);
        final ChronoUnit chronoUnit;

    }
}