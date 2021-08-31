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
                .andRoute(GET("/payment/bill/{accountNumber}"), handler::findBillByAccountNumber)
                .andRoute(GET("/payment/acquisition/{iban}"), handler::findByAcquisitionIban)
                .andRoute(GET("/payment/iban/{iban}"), handler::findPaymentByIban)
                .andRoute(GET("/payment/card/{creditCard}"), handler::findCreditCard)
                .andRoute(PUT("/payment/acquisition/update/{iban}"), handler::updateAcquisition)
                .andRoute(POST("/payment/card"), handler::makePaymentAccountNumber)
                .andRoute(POST("/payment/update"), handler::update)
                .andRoute(PUT("/payment/update/{iban}"), handler::update)
                .andRoute(POST("/payment/save"), handler::save);
    }
}
