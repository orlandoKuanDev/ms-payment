package com.example.mspayment.exception;

import com.example.mspayment.utils.I18AbleException;

public class ArgumentWebClientNotValid extends I18AbleException {
    public ArgumentWebClientNotValid(String key, Object... args) {
        super(key, args);
    }
}