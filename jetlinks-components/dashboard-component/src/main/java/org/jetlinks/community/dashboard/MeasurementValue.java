package org.jetlinks.community.dashboard;

import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;

import java.util.Comparator;

public interface MeasurementValue  extends Comparable<MeasurementValue> {

    Object getValue();

    String getTimeString();

    long getTimestamp();

    //默认排序,时间大的在前.
    @Override
    default int compareTo(MeasurementValue o) {
        return Long.compare(o.getTimestamp(), getTimestamp());
    }

    static Comparator<MeasurementValue> sort(SortOrder.Order order) {
        return order == SortOrder.Order.asc ? sort() : sortDesc();
    }

    Comparator<MeasurementValue> asc = Comparator.comparing(MeasurementValue::getTimestamp);

    Comparator<MeasurementValue> desc = asc.reversed();

    //返回排序对比器,时间小的在前
    static Comparator<MeasurementValue> sort() {
        return asc;
    }

    static Comparator<MeasurementValue> sortDesc() {
        return desc;
    }

}
