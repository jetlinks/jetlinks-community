package org.jetlinks.community.elastic.search.aggreation.bucket;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.elastic.search.aggreation.enums.OrderType;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
public class Sort {

    private String order;

    private OrderType type = OrderType.COUNT;

    private Sort() {
    }

    private Sort(String order, OrderType type) {
        this.type = type;
        this.order = order;
    }

    private Sort(String order) {
        this.order = order;
    }

    public String getOrder() {
        if ("desc".equalsIgnoreCase(order)) {
            return order;
        } else {
            return order = "asc";
        }
    }

    public static Sort asc() {
        return new Sort("asc");
    }

    public static Sort asc(OrderType type) {
        return new Sort("asc", type);
    }

    public static Sort desc() {
        return new Sort("desc");
    }

    public static Sort desc(OrderType type) {
        return new Sort("desc", type);
    }
}
