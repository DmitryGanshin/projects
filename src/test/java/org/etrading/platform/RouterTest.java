package org.etrading.platform;

import lombok.SneakyThrows;
import org.etrading.platform.dto.Order;
import org.etrading.platform.flow.AddOrderFlow;
import org.etrading.platform.repository.JdbcOrderTableOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.etrading.platform.business.Constants.*;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class RouterTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JdbcOrderTableOperations jdbcOrderTableOperations;

    @Autowired
    private AddOrderFlow addOrderFlow;

    @Value("${router.commonPath}")
    private String commonRouterPath;

    @Value("${router.user}")
    private String routerUser;

    @Value("${router.password}")
    private String routerPassword;

    @BeforeEach
    public void setup() {
        jdbcOrderTableOperations.cleanOrderTable();
    }

    @Test
    @SneakyThrows
    void addNerOrderTest() {
        BigDecimal firstOrderAmount = new BigDecimal(10000).setScale(2, RoundingMode.HALF_UP);
        BigDecimal secondOrderAmount = new BigDecimal(5000).setScale(2, RoundingMode.HALF_UP);
        BigDecimal thirdOrderAmount = new BigDecimal(5000).setScale(2, RoundingMode.HALF_UP);

        String currencyPair = "EURUSD";
        String dealtCurrency = "USD";
        String valueDate = "20250130";
        String userA = "A";
        String userB = "B";

        postOrder(Order.builder()
                .currencyPair(currencyPair)
                .dealtCurrency(dealtCurrency)
                .amount(firstOrderAmount)
                .direction(SELL)
                .valueDate(valueDate)
                .userID(userA)
                .build()).value(r -> {
                    assertEquals(firstOrderAmount, r.getAmount());
                    assertEquals(SELL, r.getDirection());
                    assertEquals(currencyPair, r.getCurrencyPair());
                    assertEquals(dealtCurrency, r.getDealtCurrency());
                    assertEquals(valueDate, r.getValueDate());
                    assertEquals(userA, r.getUserID());
                }
        );

        postOrder(Order.builder()
                .currencyPair(currencyPair)
                .dealtCurrency(dealtCurrency)
                .amount(secondOrderAmount)
                .direction(BUY)
                .valueDate(valueDate)
                .userID(userA)
                .build()).value(r -> {
                    assertEquals(firstOrderAmount.subtract(secondOrderAmount), r.getAmount());
                    assertEquals(SELL, r.getDirection());
                    assertEquals(currencyPair, r.getCurrencyPair());
                    assertEquals(dealtCurrency, r.getDealtCurrency());
                    assertEquals(valueDate, r.getValueDate());
                    assertEquals(userA, r.getUserID());
                }
        );

        postOrder(Order.builder()
                .currencyPair(currencyPair)
                .dealtCurrency(dealtCurrency)
                .amount(thirdOrderAmount)
                .direction(BUY)
                .valueDate(valueDate)
                .userID(userB)
                .build()).value(r -> {
                    assertEquals(thirdOrderAmount, r.getAmount());
                    assertEquals(BUY, r.getDirection());
                    assertEquals(currencyPair, r.getCurrencyPair());
                    assertEquals(dealtCurrency, r.getDealtCurrency());
                    assertEquals(valueDate, r.getValueDate());
                    assertEquals(userB, r.getUserID());
                }
        );
    }

    @Test
    public void getMatchingAmountTest() {
        addOrderFlow.addNewOrder(prepareOrder("A", BigDecimal.valueOf(500), SELL));
        assertEquals(expectedMatchedPct("0"), getMatchingAmount("A").get(0));

        addOrderFlow.addNewOrder(prepareOrder("B", BigDecimal.valueOf(200), BUY));
        assertEquals(expectedMatchedPct("40"), getMatchingAmount("A").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("B").get(0));

        addOrderFlow.addNewOrder(prepareOrder("C", BigDecimal.valueOf(100), BUY));
        assertEquals(expectedMatchedPct("60"), getMatchingAmount("A").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("B").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("C").get(0));

        addOrderFlow.addNewOrder(prepareOrder("D", BigDecimal.valueOf(100), SELL));
        assertEquals(expectedMatchedPct("60"), getMatchingAmount("A").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("B").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("C").get(0));
        assertEquals(expectedMatchedPct("0"), getMatchingAmount("D").get(0));

        addOrderFlow.addNewOrder(prepareOrder("E", BigDecimal.valueOf(100), SELL));
        assertEquals(expectedMatchedPct("60"), getMatchingAmount("A").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("B").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("C").get(0));
        assertEquals(expectedMatchedPct("0"), getMatchingAmount("D").get(0));
        assertEquals(expectedMatchedPct("0"), getMatchingAmount("E").get(0));

        addOrderFlow.addNewOrder(prepareOrder("F", BigDecimal.valueOf(1000), SELL));
        assertEquals(expectedMatchedPct("60"), getMatchingAmount("A").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("B").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("C").get(0));
        assertEquals(expectedMatchedPct("0"), getMatchingAmount("D").get(0));
        assertEquals(expectedMatchedPct("0"), getMatchingAmount("E").get(0));
        assertEquals(expectedMatchedPct("0"), getMatchingAmount("F").get(0));


        addOrderFlow.addNewOrder(prepareOrder("G", BigDecimal.valueOf(700),  BUY));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("A").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("B").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("C").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("D").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("E").get(0));
        assertEquals(expectedMatchedPct("30"), getMatchingAmount("F").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("G").get(0));

        addOrderFlow.addNewOrder(prepareOrder("G", BigDecimal.valueOf(1000),  BUY));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("A").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("B").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("C").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("D").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("E").get(0));
        assertEquals(expectedMatchedPct("100"), getMatchingAmount("F").get(0));

        List<BigDecimal> userGMatchingAmount = getMatchingAmount("G");
        assertEquals(expectedMatchedPct("100"), userGMatchingAmount.get(0));
        assertEquals(expectedMatchedPct("70"), userGMatchingAmount.get(1));

    }

    private BigDecimal expectedMatchedPct(String pct) {
        return new BigDecimal(pct);
    }

    private Order prepareOrder(String user, BigDecimal amount, String direction) {
        return Order.builder()
                .currencyPair("EURUSD")
                .dealtCurrency("USD")
                .amount(amount)
                .direction(direction)
                .valueDate("20250130")
                .userID(user)
                .build();
    }

    private WebTestClient.BodySpec<Order, ?> postOrder(Order postOrder) {
        return webTestClient.post()
                .uri(commonRouterPath)
                .headers(h -> h.setBasicAuth(routerUser, routerPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(postOrder), Order.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Order.class);
    }

    private List<BigDecimal> getMatchingAmount(String IserID) {
        return webTestClient.get()
                .uri(commonRouterPath+"/"+IserID)
                .headers(h -> h.setBasicAuth(routerUser, routerPassword))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk().expectBodyList(BigDecimal.class)
                .returnResult()
                .getResponseBody();
    }
}
