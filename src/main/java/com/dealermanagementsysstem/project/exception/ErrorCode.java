package com.dealermanagementsysstem.project.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    DUPLICATE_EMAIL("CO02","Email adready exists.","/customer/create"),
    INVALID_TESTDRIVE_DATE("T001","Invalid test drive date","/customer/create"),
    DATABASE_ERROR("SYS001","Database error",null),
    CUSTOMER_NOT_FOUND("C001","Customer not Found","/customer/list"),
    CUSTOMER_UPDATE_FAILED("C003", "Failed to update customer", "dealerPage/customerEdit"),
    ;

    private final String code;
    private final String message;
    private final String redirectPath;

    ErrorCode(String code, String message, String redirectPath) {
        this.code = code;
        this.message = message;
        this.redirectPath = redirectPath;
    }

}
