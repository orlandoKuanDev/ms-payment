package com.example.mspayment.services;

import com.example.mspayment.models.entities.Payment;
import reactor.core.publisher.Mono;

public interface IPaymentService extends IBaseService<Payment, String> {
    Mono<Payment> findByAcquisition_Iban(String iban);
}
