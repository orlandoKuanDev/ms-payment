package com.example.mspayment.models.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class PaymentCreateDTO {
    private Double amount;
    private String description;
    private String cardNumber;
    private String iban;
}
