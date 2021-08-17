package com.example.mspayment.config;

import com.example.mspayment.handler.PaymentHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterConfig {
    @Bean
    public RouterFunction<ServerResponse> rutas(PaymentHandler handler){
        return route(GET("/payment"), handler::findAll)
                .andRoute(GET("/payment/{id}"), handler::findById)
                .andRoute(POST("/payment"), handler::save);
    }
}