package com.example.mspayment.models.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Acquisition {
    @Field(name = "product")
    private Product product;

    @Field(name = "initial")
    private double initial;

    @Field(name = "debt")
    private double debt;

    @Field(name = "cardNumber")
    private String cardNumber;

    @Field(name = "bill")
    private Bill bill;

}
