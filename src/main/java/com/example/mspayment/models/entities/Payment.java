package com.example.mspayment.models.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "payment")
@Data
public class Payment {
    @Id
    private String id;

    @Field(name = "amount")
    private Double amount;

    @Field(name = "acquisition")
    private Acquisition acquisition;

    @Field(name = "description")
    private String description;

    @Field(name = "paymentDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentDate = LocalDateTime.now();
}
