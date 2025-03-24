package org.etrading.platform.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.etrading.platform.business.OrderCalculations;
import org.etrading.platform.dto.Matching;
import org.etrading.platform.dto.Order;
import org.etrading.platform.repository.JdbcOrderTableOperations;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.etrading.platform.business.Constants.*;

@Component
@RequiredArgsConstructor
@Log4j2
public class AddOrderFlow {

    private final JdbcOrderTableOperations jdbcOrderTableOperations;
    private final OrderCalculations orderCalculations;

    public Order addNewOrder(Order newOrder) {
        Order aggregatedOrder = Order.builder().build();
        try {
            newOrder.setId(jdbcOrderTableOperations.createOrderId());
            jdbcOrderTableOperations.addOrder(newOrder);
            addNewOrderMatchingAmount(newOrder);
            aggregatedOrder = orderCalculations.getAggregatedOrder(jdbcOrderTableOperations.findOrdersForAggregation(newOrder));
        } catch (Exception e) {
            log.error("Error while working on the new order flow", e);
        }

        return aggregatedOrder;
    }

    private void addNewOrderMatchingAmount(Order newOrder) {
        List<Matching> matchWithOrders = jdbcOrderTableOperations.findPreviousOrdersForMatching(newOrder.getId(), BUY.equals(newOrder.getDirection()) ? SELL : BUY);
        orderCalculations.calculateMatchingAmount(newOrder, matchWithOrders).forEach(jdbcOrderTableOperations::updateOrderMatchingPct);
    }
}
