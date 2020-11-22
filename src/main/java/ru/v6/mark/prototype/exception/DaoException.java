package ru.v6.mark.prototype.exception;

public class DaoException extends ApplicationException {
    private static final String DEFAULT_MESSAGE = "error.dao.default";

    public DaoException(String message) {
        super(message);
    }

    public DaoException(Throwable cause) {
        super(cause, DEFAULT_MESSAGE);
        parameters(cause.getMessage());
    }

    public DaoException(Throwable cause, String message) {
        super(cause, message);
    }

    public DaoException(Throwable cause, String message, Object... parameters) {
        super(cause, message);
        parameters(parameters);
    }

}
