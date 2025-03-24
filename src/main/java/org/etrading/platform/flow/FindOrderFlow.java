package org.etrading.platform.flow;

import lombok.RequiredArgsConstructor;
import org.etrading.platform.repository.JdbcOrderTableOperations;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FindOrderFlow {

    private final JdbcOrderTableOperations jdbcOrderTableOperations;

    public List<BigDecimal> findMatchingAmount(String userId) {
        return jdbcOrderTableOperations.findMatchingAmountByUser(userId);
    }
}
