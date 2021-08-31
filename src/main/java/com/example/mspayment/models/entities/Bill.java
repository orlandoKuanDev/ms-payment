package com.example.mspayment.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

//@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Bill {
    @Field(name = "accountNumber")
    private String accountNumber;
    @Field(name = "balance")
    private Double balance;
    //@Field(name = "acquisition")
    //private Acquisition acquisition;
}