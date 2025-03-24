package org.etrading.platform.repository;

import lombok.RequiredArgsConstructor;
import org.etrading.platform.dto.Matching;
import org.etrading.platform.dto.Order;
import org.etrading.platform.dto.OrderAggregationAttributes;
import org.etrading.platform.dto.OrdersToAggregate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static java.sql.Types.*;

@Repository
@RequiredArgsConstructor
public class JdbcOrderTableOperations {
    private final JdbcOperations jdbcOperations;

    public void addOrder(Order order) {
        jdbcOperations.update("insert into orders(id, currencyPair, dealtCurrency, direction, amount, valueDate, userId) values(?,?,?,?,?,?,?)",
                new Object[]{order.getId(), order.getCurrencyPair(), order.getDealtCurrency(), order.getDirection(), order.getAmount(), order.getValueDate(), order.getUserID()},
                new int[]{INTEGER, VARCHAR, VARCHAR, VARCHAR, DECIMAL, VARCHAR, VARCHAR}
        );
    }

    public Integer createOrderId() {
        return jdbcOperations.queryForObject("select nextval('orders_seq')", Integer.class);
    }

    public OrdersToAggregate findOrdersForAggregation(Order order) {
        return OrdersToAggregate.builder()
                .attributesForAggregation(jdbcOperations.query("select direction, amount from orders where currencyPair = ? and dealtCurrency = ? and valueDate = ? and userId = ?",
                        new Object[]{order.getCurrencyPair(), order.getDealtCurrency(), order.getValueDate(), order.getUserID()},
                        new int[]{VARCHAR, VARCHAR, VARCHAR, VARCHAR},
                        BeanPropertyRowMapper.newInstance(OrderAggregationAttributes.class)
                )).incomingOrder(order).build();
    }

    public List<Matching> findPreviousOrdersForMatching(Integer newOrderId, String direction) {
        return jdbcOperations.query("select id, amount, matchedPct, matchedAmount from orders where direction = ? and id < ? and matchedPct < 1 order by id asc",
                new Object[]{direction, newOrderId},
                new int[]{VARCHAR, INTEGER},
                BeanPropertyRowMapper.newInstance(Matching.class));
    }

    public void updateOrderMatchingPct(Matching matching) {
        jdbcOperations.update("update orders set matchedPct = ?, matchedAmount = ? where id = ?",
                new Object[]{matching.getMatchedPct(), matching.getMatchedAmount(), matching.getId()},
                new int[]{DECIMAL, DECIMAL, INTEGER}
        );

    }

    public List<BigDecimal> findMatchingAmountByUser(String userID) {
        return jdbcOperations.query("select matchedPct from orders where userId = ? order by id asc",
                new Object[]{userID},
                new int[]{VARCHAR},
                (rs, n) -> rs.getBigDecimal("matchedPct").multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP));
    }

    public void cleanOrderTable() {
        jdbcOperations.update("truncate table orders");
    }
}
