package com.example.mspayment.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CreatePaymentWithAccountDTO {
    private Double amount;
    private String description;
    private String accountNumber;
    private String creditCard;
}