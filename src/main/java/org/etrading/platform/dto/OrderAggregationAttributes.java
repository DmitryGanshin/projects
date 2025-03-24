package org.etrading.platform.dto;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderAggregationAttributes {
    private String direction;
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;
}
