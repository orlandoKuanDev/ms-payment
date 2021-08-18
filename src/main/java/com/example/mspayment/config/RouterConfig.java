package com.example.mspayment.config;

import com.example.mspayment.handler.PaymentHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterConfig {
    @Bean
    public RouterFunction<ServerResponse> rutas(PaymentHandler handler){
        return route(GET("/payment"), handler::findAll)
                .andRoute(GET("/payment/{id}"), handler::findById)
                .andRoute(GET("/payment/acquisition/{cardNumber}"), handler::findByAcquisitionCardNumber)
                .andRoute(PUT("/payment/acquisition/update/{cardNumber}"), handler::updateAcquisition)
                .andRoute(PUT("/payment/update/{id}"), handler::update)
                .andRoute(POST("/payment"), handler::save);
    }
}
