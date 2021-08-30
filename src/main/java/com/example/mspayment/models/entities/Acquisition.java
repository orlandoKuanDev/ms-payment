package com.example.mspayment.models.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Acquisition {

    @Field(name = "product")
    private Product product;

    @Field(name = "customerOwner")
    private List<Customer> customerHolder;

    @Field(name = "authorizedSigner")
    private List<Customer> customerAuthorizedSigner;

    @Field(name = "initial")
    private double initial;

    @Field(name = "iban")
    private String iban;

    @Field(name = "cardNumber")
    private String cardNumber;

    @Field(name = "bill")
    private Bill bill;
}
