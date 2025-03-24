package org.etrading.platform.router;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.etrading.platform.dto.Order;
import org.etrading.platform.flow.AddOrderFlow;
import org.etrading.platform.flow.FindOrderFlow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;

@Service
@RequiredArgsConstructor
@Log4j2
public class RouterHandlers {

    @Value("${router.pathvariable.userid}")
    private String pathVariableUserID;
    private final AddOrderFlow addOrderFlow;
    private final FindOrderFlow findOrderFlow;

    public Mono<ServerResponse> processNewOrder(ServerRequest serverRequest) {
        return ServerResponse.ok().contentType(APPLICATION_JSON).body(
                fromPublisher(serverRequest
                        .bodyToMono(Order.class)
                        .onErrorReturn(Order.builder().build())
                        .doOnSuccess(this::orderSuccess)
                        .flatMap(this::newOrderToMono), Order.class)
        );
    }

    public Mono<ServerResponse> getMatchingAmountForUserId(ServerRequest serverRequest) {
        return ServerResponse.ok().contentType(APPLICATION_JSON)
                .bodyValue(matchingAmountFlow(serverRequest.pathVariable(pathVariableUserID)));
    }

    private Mono<Order> newOrderToMono(Order newOrder) {
        return Mono.just(addOrderFlow.addNewOrder(newOrder));
    }

    private List<BigDecimal> matchingAmountFlow(String userId) {
        return findOrderFlow.findMatchingAmount(userId);
    }

    private void orderSuccess(Order order) {
        log.info("Successfully received and deserialized order. {}", order);
    }
}
