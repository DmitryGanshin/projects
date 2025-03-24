package org.etrading.platform.router;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@EnableWebFluxSecurity
@Configuration
public class RoutersConfiguration {

    @Bean
    public RouterFunction<ServerResponse> routes(RouterHandlers routerHandlers,
                                                 @Value("${router.commonPath}") String commonRouterPath,
                                                 @Value("${router.pathvariable.userid}") String pathVariableUserID) {
        return RouterFunctions.route().path(commonRouterPath, b -> b
                .POST(accept(MediaType.APPLICATION_JSON), routerHandlers::processNewOrder)
                .GET("/{"+pathVariableUserID+"}", routerHandlers::getMatchingAmountForUserId)
        ).build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(@Value("${router.user}") String routerUser,
                                                            @Value("${router.password}") String routerPassword) {
        return new MapReactiveUserDetailsService(
                User.withUsername(routerUser)
                        .password(passwordEncoder().encode(routerPassword))
                        .roles("USER")
                        .build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
      return http.authorizeExchange(auth -> auth.anyExchange().authenticated()).httpBasic(Customizer.withDefaults()).csrf(ServerHttpSecurity.CsrfSpec::disable).build();
    }
}
