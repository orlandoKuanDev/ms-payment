package com.example.mspayment.services;

import com.example.mspayment.models.entities.Payment;
import com.example.mspayment.repositories.IPaymentRepository;
import com.example.mspayment.repositories.IRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
