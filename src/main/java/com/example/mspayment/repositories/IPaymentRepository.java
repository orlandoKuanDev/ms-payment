package com.example.mspayment.repositories;

import com.example.mspayment.models.entities.Payment;
import reactor.core.publisher.Mono;

public interface IPaymentRepository extends IRepository<Payment, String>{
    Mono<Payment> findByAcquisition_Iban(String iban);
    Mono<Void> deleteByAcquisition_Iban(String iban);
    Mono<Payment> findByAcquisition_Bill_AccountNumber(String accountNumber);
    Mono<Payment> findByAcquisition_CardNumber(String creditCard);
}
