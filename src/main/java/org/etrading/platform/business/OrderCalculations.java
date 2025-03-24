package org.etrading.platform.business;

import org.etrading.platform.dto.Matching;
import org.etrading.platform.dto.Order;
import org.etrading.platform.dto.OrderAggregationAttributes;
import org.etrading.platform.dto.OrdersToAggregate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.etrading.platform.business.Constants.*;

@Service
public class OrderCalculations {

    public Order getAggregatedOrder(OrdersToAggregate ordersToAggregate) {
        Order incomingOrder = ordersToAggregate.getIncomingOrder();
        OrderAggregationAttributes initialIdentity = OrderAggregationAttributes.builder().direction(incomingOrder.getDirection()).build();
        OrderAggregationAttributes aggregationAttributes = ordersToAggregate.getAttributesForAggregation().stream().reduce(initialIdentity, this::aggregationCalculation);

        return Order.builder()
                .currencyPair(incomingOrder.getCurrencyPair())
                .dealtCurrency(incomingOrder.getDealtCurrency())
                .valueDate(incomingOrder.getValueDate())
                .userID(incomingOrder.getUserID())
                .direction(aggregationAttributes.getDirection())
                .amount(aggregationAttributes.getAmount())
                .build();
    }

    private OrderAggregationAttributes aggregationCalculation(OrderAggregationAttributes subtotal, OrderAggregationAttributes element) {
        int subtotalSign = getDirection(subtotal.getDirection());
        int elementSign = getDirection(element.getDirection());

        BigDecimal subtotalAmount = subtotal.getAmount().multiply(BigDecimal.valueOf(subtotalSign));
        BigDecimal elementAmount = element.getAmount().multiply(BigDecimal.valueOf(elementSign));
        BigDecimal aggregatedAmount = subtotalAmount.add(elementAmount);

        String directionForAggregatedAmount = subtotal.getDirection();
        if (aggregatedAmount.compareTo(BigDecimal.ZERO) < 0)
            directionForAggregatedAmount = BUY;
        if (aggregatedAmount.compareTo(BigDecimal.ZERO) > 0)
            directionForAggregatedAmount = SELL;

        return OrderAggregationAttributes.builder()
                .amount(aggregatedAmount.abs())
                .direction(directionForAggregatedAmount)
                .build();
    }

    private int getDirection(String direction) {
        return BUY.equals(direction) ? -1 : 1;
    }

    public List<Matching> calculateMatchingAmount(Order orderToMatch, List<Matching> matchWithOrders) {
        List<Matching> listOfMatchedAmount = new ArrayList<>();

        BigDecimal newOrderAmountToMatch = orderToMatch.getAmount();
        BigDecimal newOrderPctMatched = BigDecimal.ZERO;;
        BigDecimal newOrderMatchedAmount = BigDecimal.ZERO;
        BigDecimal newOrderDeltaAmount = newOrderAmountToMatch;
        Integer newOrderId = orderToMatch.getId();

        for (Matching matchWithOrder : matchWithOrders) {

            BigDecimal existentOrderAmount = matchWithOrder.getAmount();
            BigDecimal existentOrderMatchedAmount = matchWithOrder.getMatchedAmount();
            BigDecimal existentOrderDeltaAmount = existentOrderAmount.subtract(existentOrderMatchedAmount);
            BigDecimal existentOrderMatchedPct;
            Integer existentOrderId = matchWithOrder.getId();

            if (existentOrderDeltaAmount.compareTo(newOrderDeltaAmount) >= 0) {
                existentOrderMatchedPct = newOrderDeltaAmount.add(existentOrderMatchedAmount).divide(existentOrderAmount,2, RoundingMode.HALF_UP);
                listOfMatchedAmount.add(Matching.builder().matchedPct(BigDecimal.valueOf(1)).id(newOrderId).matchedAmount(newOrderAmountToMatch).build());
                listOfMatchedAmount.add(Matching.builder().matchedPct(existentOrderMatchedPct).id(existentOrderId).matchedAmount(existentOrderMatchedAmount.add(newOrderDeltaAmount)).build());

                return listOfMatchedAmount;
            } else {

                newOrderMatchedAmount = newOrderMatchedAmount.add(existentOrderDeltaAmount);
                newOrderDeltaAmount = newOrderDeltaAmount.subtract(existentOrderDeltaAmount);
                newOrderPctMatched  = newOrderMatchedAmount.divide(newOrderAmountToMatch, 2, RoundingMode.HALF_UP);

                listOfMatchedAmount.add(Matching.builder().matchedPct(BigDecimal.valueOf(1)).id(existentOrderId).matchedAmount(existentOrderAmount).build());
            }
        }

        if (!matchWithOrders.isEmpty())
          listOfMatchedAmount.add(Matching.builder().id(newOrderId).matchedPct(newOrderPctMatched).matchedAmount(newOrderMatchedAmount).build());

        return listOfMatchedAmount;
    }

}
