package org.jetlinks.community.notify.manager.subscriber;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor(staticName = "of")
@Getter
@Setter
@NoArgsConstructor
public class Notify {
    private String message;

    private String dataId;

    private long notifyTime;


    private String code;

    private Object detail;


    public static Notify of(String message, String dataId, long timestamp) {
        return new Notify(message, dataId, timestamp, null,null);
    }
}
