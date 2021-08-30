package com.example.mspayment.services;

import com.example.mspayment.models.entities.Payment;
import com.example.mspayment.repositories.IPaymentRepository;
import com.example.mspayment.repositories.IRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PaymentService extends BaseService<Payment, String> implements IPaymentService{
   private final IPaymentRepository paymentRepository;

    @Autowired
    public PaymentService(IPaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    protected IRepository<Payment, String> getRepository() {
        return paymentRepository;
    }

    @Override
    public Mono<Payment> findByAcquisition_Iban(String iban) {
        return paymentRepository.findByAcquisition_Iban(iban);
    }

    @Override
    public Mono<Void> deleteByAcquisition_Iban(String iban) {
        return paymentRepository.deleteByAcquisition_Iban(iban);
    }

    @Override
    public Mono<Payment> findByAcquisition_Bill_AccountNumber(String accountNumber) {
        return paymentRepository.findByAcquisition_Bill_AccountNumber(accountNumber);
    }

    @Override
    public Mono<Payment> findByAcquisition_CardNumber(String creditCard) {
        return paymentRepository.findByAcquisition_CardNumber(creditCard);
    }
}
