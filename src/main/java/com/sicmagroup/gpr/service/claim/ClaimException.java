package com.sicmagroup.gpr.service.claim;

public class ClaimException extends Exception{
    
    public ClaimException(String message){
        super(message);
    }

    public ClaimException(String message, Throwable cause){
        super(message, cause);
    }
}
