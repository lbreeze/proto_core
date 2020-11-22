package ru.v6.mark.prototype.exception;

import ru.v6.mark.prototype.domain.constant.Status;

import java.io.Serializable;

public class ApplicationException extends RuntimeException {
    private Status status = Status.ERROR_COMMON;
    private static final String DEFAULT_MESSAGE = "error.system.default";
    private Object[] parameters = null;
    private Serializable identity = null;

    protected ApplicationException(String message) {
        super(message);
    }

    protected ApplicationException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }

    protected ApplicationException(Throwable cause, String message) {
        super(message, cause);
    }

    @Deprecated
    protected ApplicationException(String message, Object... parameters) {
        super(message);
        this.parameters = parameters;
    }

    public static ApplicationException build(String message) {
        return new ApplicationException(message);
    }

    public static ApplicationException build(Throwable cause) {
        return new ApplicationException(cause);
    }

    public static ApplicationException build(Throwable cause, String message) {
        return new ApplicationException(cause, message);
    }

    public ApplicationException parameters(Object... parameters) {
        this.parameters = parameters;
        return this;
    }

    public ApplicationException status(Status status) {
        this.status = status;
        return this;
    }

    public ApplicationException identity(Serializable identity) {
        this.identity = identity;
        return this;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public Status getStatus() {
        return status;
    }

    public Serializable getIdentity() {
        return identity;
    }

}
