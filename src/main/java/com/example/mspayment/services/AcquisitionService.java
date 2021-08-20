package com.example.mspayment.services;

import com.example.mspayment.models.entities.Acquisition;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@Slf4j(topic = "ACQUISITION_WEBCLIENT_SERVICE")
public class AcquisitionService {
    private final WebClient.Builder webClientBuilder;

    Logger logger = LoggerFactory.getLogger(AcquisitionService.class);

    @Autowired
    public AcquisitionService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<Acquisition> findByBillAccountNumber(String accountNumber) {
        return webClientBuilder
                .baseUrl("http://SERVICE-ACQUISITION/acquisition")
                .build()
                .get()
                .uri("/bill/{accountNumber}", Collections.singletonMap("accountNumber", accountNumber))
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE ACQUISITION FIND FAILED"));
                })
                .bodyToMono(Acquisition.class);
    }

    public Mono<Acquisition> findByIban(String iban) {
        return webClientBuilder
                .baseUrl("http://SERVICE-ACQUISITION/acquisition")
                .build()
                .get()
                .uri("/card/{iban}", Collections.singletonMap("iban", iban))
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE ACQUISITION FIND FAILED"));
                })
                .bodyToMono(Acquisition.class);
    }

    public Mono<Acquisition> updateAcquisition(Acquisition acquisition, String iban){
        return webClientBuilder
                .baseUrl("http://SERVICE-ACQUISITION/acquisition")
                .build()
                .put()
                .uri("/update/{iban}", iban)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(acquisition), Acquisition.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE ACQUISITION UPDATE FAILED"));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    return Mono.error(new RuntimeException("THE ACQUISITION UPDATE FAILED"));
                })
                .bodyToMono(Acquisition.class);
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
