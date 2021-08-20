package com.example.mspayment.models.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Customer {

    @Field(name = "customerIdentityType")
    private String customerIdentityType;
    @Field(name = "customerIdentityNumber")
    private String customerIdentityNumber;
    @Size(max = 40)
    @Field(name = "name")
    private String name;
    @Size(max = 75)
    @Email
    @Field(name = "email")
    private String email;
    @Size(max = 9)
    @Field(name = "phone")
    private String phone;
    @Field(name = "address")
    private String address;
    @Field(name = "customerType")
    private String customerType;
}
