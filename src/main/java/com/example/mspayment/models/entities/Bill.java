package com.example.mspayment.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Bill {
    @Field(name = "accountNumber")
    private String accountNumber;

    @Field(name = "balance")
    private Double balance;
}