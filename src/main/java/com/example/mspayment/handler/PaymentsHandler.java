package com.example.mspayment.handler;

import com.example.mspayment.models.entities.Payment;
import com.example.mspayment.services.CustomerService;
import com.example.mspayment.services.IPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Component
public class PaymentsHandler {
    private final IPaymentService iConsuptionService;
    private final CustomerService customerService;
    @Autowired
    public PaymentsHandler(IPaymentService iConsuptionService, CustomerService customerService) {
        //log.info("ConsuptionsHandler");
        this.iConsuptionService = iConsuptionService;
        this.customerService = customerService;
    }
    public Mono<ServerResponse> findAll(ServerRequest request){
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(iConsuptionService.findAll(), Payment.class);
    }
    public Mono<ServerResponse> findById(ServerRequest request){
        String productId = request.pathVariable("productId");
        return iConsuptionService.findById(productId).flatMap(p -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(p))
                        .switchIfEmpty(ServerResponse.notFound().build()
        );
    }
    public Mono<ServerResponse> findByIdentityNumber(ServerRequest request){
        String identityNumber = request.pathVariable("customerIdentityNumber");
        return customerService.findByIdentityNumber(identityNumber).flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(Mono.error(new RuntimeException("THE PRODUCT DOES NOT EXIST")));
    }
    public Mono<ServerResponse> save(ServerRequest request){
        log.info("LLEGO A C");
        Payment pay= new Payment();
        Mono<Payment> payment = request.bodyToMono(Payment.class);
        //String[] idCustomer = new String[1];
        payment.flatMap(payment1->{
            //idCustomer[0] = payment1.getCustomerIdentityNumber();
            return customerService.findByIdentityNumber(payment1.getCustomerIdentityNumber())
                .flatMap(customer -> {
                    pay.setCustomerIdentityNumber(customer.getCustomerIdentityNumber());
                    return Mono.just(pay);
                });
        });
        //log.info("IDCUSTOMER: "+idCustomer[0]);
        return payment.flatMap(iConsuptionService::create)
                .flatMap(p -> {
                        return ServerResponse.created(URI.create("/api/client/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p);
                        })
                .onErrorResume(error -> {
                    WebClientResponseException errorResponse = (WebClientResponseException) error;
                    if(errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(errorResponse.getResponseBodyAsString());
                    }
                    return Mono.error(errorResponse);
                });
    }

}
