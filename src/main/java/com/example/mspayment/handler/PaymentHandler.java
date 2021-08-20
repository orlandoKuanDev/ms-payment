package com.example.mspayment.handler;

import com.example.mspayment.models.entities.Acquisition;
import com.example.mspayment.models.entities.Payment;
import com.example.mspayment.services.AcquisitionService;
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
import java.time.LocalDateTime;
import java.util.Objects;

@Component
@Slf4j(topic = "PAYMENT_HANDLER")
public class PaymentHandler {
    private final IPaymentService paymentService;
    private final AcquisitionService acquisitionService;

    @Autowired
    public PaymentHandler(IPaymentService paymentService, AcquisitionService acquisitionService) {
        this.paymentService = paymentService;
        this.acquisitionService = acquisitionService;
    }


    public Mono<ServerResponse> findAll(ServerRequest request){
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(paymentService.findAll(), Payment.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("id");
        return paymentService.findById(id).flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(Mono.error(new RuntimeException("Payment no found")));
    }

    public Mono<ServerResponse> findByAcquisitionIban(ServerRequest request){
        String iban = request.pathVariable("iban");
        return acquisitionService.findByIban(iban).flatMap(p -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(p))
                        .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> updateAcquisition(ServerRequest request){
        Mono<Acquisition> acquisition = request.bodyToMono(Acquisition.class);
        String iban = request.pathVariable("iban");
        return acquisition.flatMap(acquisition1 -> acquisitionService.updateAcquisition(acquisition1, iban)).flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> save(ServerRequest request){
        Mono<Payment> payment = request.bodyToMono(Payment.class);
        return payment.flatMap(paymentService::create)
                .flatMap(p -> ServerResponse.created(URI.create("/payment/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .onErrorResume(error -> Mono.error(new RuntimeException(error.getMessage())));
    }

    public Mono<ServerResponse> makePayment(ServerRequest request){
        Mono<Payment> payment = request.bodyToMono(Payment.class);
        Payment paymentDto = new Payment();

        return payment.flatMap(paymentRequest -> {
                    paymentDto.setDescription(paymentRequest.getDescription());
                    paymentDto.setAmount(paymentRequest.getAmount());
                    return acquisitionService.findByBillAccountNumber(paymentRequest.getAcquisition().getBill().getAccountNumber());
                }) .checkpoint("after consultation acquisition service web-client by account number")
                .flatMap(acquisition -> {
                    // FALTA EVALUACION DE MULTIPLES CUENTAS ASOCIADAS A UNA TARJETA DE CREDITO.
                    // PAGAR DESDE UNA SOLA CUENTA
                    if (Objects.equals(acquisition.getBill().getBalance(), paymentDto.getCreditLine())){
                        //return Mono.error(new RuntimeException("you are up to date on your payments"));
                        return ServerResponse.created(URI.create("/payment/".concat(paymentDto.getId())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue("you are up to date on your payments");
                    }
                    acquisition.getBill().setBalance(paymentDto.getAmount() - acquisition.getBill().getBalance());
                    paymentDto.setAcquisition(acquisition);
                    paymentDto.setPaymentDate(LocalDateTime.now());
                    return paymentService.create(paymentDto);
                }).flatMap(p -> ServerResponse.created(URI.create("/payment/".concat(paymentDto.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(paymentDto));
    }

    public Mono<ServerResponse> update(ServerRequest request){
        Mono<Payment> payment = request.bodyToMono(Payment.class);
        String id = request.pathVariable("id");
        Mono<Payment> paymentDB = paymentService.findById(id);
        return paymentDB.zipWith(payment, (db, req) -> {
           db.setAmount(req.getAmount());
           db.setDescription(req.getDescription());
           return db;
        }).flatMap(paymentService::update).flatMap(p -> ServerResponse.created(URI.create("/payment/".concat(p.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(p));
    }
}
