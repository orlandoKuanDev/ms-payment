package com.example.mspayment.services;

import com.example.mspayment.exception.ArgumentWebClientNotValid;
import com.example.mspayment.models.entities.Customer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Service
public class CustomerService {
    private final WebClient.Builder webClientBuilder;

    Logger logger = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    public CustomerService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<Customer> findByIdentityNumber(String identityNumber) {
        log.info("identityNumber: "+identityNumber);
        return webClientBuilder
                .baseUrl("http://SERVICE-CUSTOMER/customer")
                .build()
                .get()
                .uri("/identity/{identityNumber}", Collections.singletonMap("identityNumber", identityNumber))
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new ArgumentWebClientNotValid(
                            String.format("THE PRODUCT NAME DONT EXIST IN MICRO SERVICE PRODUCT-> %s", identityNumber)
                    ));
                })
                .bodyToMono(Customer.class);
    }

    public static void logTraceResponse(Logger log, ClientResponse response) {
        if (log.isTraceEnabled()) {
            log.trace("Response status: {}", response.statusCode());
            log.trace("Response headers: {}", response.headers().asHttpHeaders());
            response.bodyToMono(String.class)
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(body -> log.trace("Response body: {}", body));
        }
    }
}
