package org.kosa.userservice.exception;

public class AlreadyWithdrawnException extends RuntimeException {
    public AlreadyWithdrawnException(String message) {
        super(message);
    }

    public AlreadyWithdrawnException(String message, Throwable cause) {
        super(message, cause);
    }
}