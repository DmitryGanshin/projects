package org.etrading.platform.dto;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Matching {
    private Integer id;
    private BigDecimal amount;
    private BigDecimal matchedPct;
    private BigDecimal matchedAmount;
}
