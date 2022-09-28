package org.jetlinks.community.dashboard.measurements.sys;

import java.io.Serializable;

public interface MonitorInfo<T extends MonitorInfo<T>> extends Serializable {


    T add(T info);


    T division(int num);

}
