package com.sicmagroup.gpr.service.media;

public class FileStorageException extends RuntimeException {    
    private static long serialVersionUID = 1L;

    public FileStorageException(String message){
        super(message);
    }

    public FileStorageException(String message, Throwable cause){
        super(message, cause);
    }


}
