package org.etrading.platform.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class Order {
    private int id;
    private String currencyPair;
    private String dealtCurrency;
    private String direction;
    private BigDecimal amount;
    private String valueDate;
    private String userID;
}
