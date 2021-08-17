package com.example.mspayment.handler;

import org.springframework.data.mongodb.core.mapping.Field;

public class Acquisition {
    @Field(name = "initial")
    private double initial;

    @Field(name = "debt")
    private double debt;

    @Field(name = "cardNumber")
    private String cardNumber;
}
