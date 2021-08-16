package com.example.mspayment.config;

import com.example.mspayment.handler.PaymentsHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Slf4j
@Configuration
public class RouterConfig {
    @Bean
    public RouterFunction<ServerResponse> rutas(PaymentsHandler handler){
        log.info("PaymentsHandler");
        return route(GET("/payments"), handler::findAll)
                .andRoute(GET("/payments/{id}"), handler::findById)
                .andRoute(POST("/payments"), handler::save);
    }
}
