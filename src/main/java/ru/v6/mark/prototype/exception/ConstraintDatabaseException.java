package ru.v6.mark.prototype.exception;

public class ConstraintDatabaseException extends DaoException {
    private String constraintName;

    public ConstraintDatabaseException(String message) {
        super(message);
    }

    public ConstraintDatabaseException(Throwable cause, String message) {
        super(cause, message);
    }

    public ConstraintDatabaseException(Throwable cause, String message, String constraintName) {
        super(cause, message, constraintName);
        this.constraintName = constraintName;
    }

    public String getConstraintName() {
        return constraintName;
    }

}
