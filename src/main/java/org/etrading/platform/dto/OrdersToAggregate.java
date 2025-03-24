package org.etrading.platform.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder(toBuilder = true)
@Getter
public class OrdersToAggregate {
    private final Order incomingOrder;
    private final List<OrderAggregationAttributes> attributesForAggregation;
}
