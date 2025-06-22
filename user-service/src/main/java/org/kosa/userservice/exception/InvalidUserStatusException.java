package org.kosa.userservice.exception;

public class InvalidUserStatusException extends RuntimeException {
    public InvalidUserStatusException(String message) {
        super(message);
    }

    public InvalidUserStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
