package org.jetlinks.community;

import java.time.ZonedDateTime;
import java.util.Iterator;

public interface TimerIterable {

    Iterator<ZonedDateTime> iterator(ZonedDateTime baseTime);

}
