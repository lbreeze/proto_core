package ru.v6.mark.prototype.exception;

import ru.v6.mark.prototype.support.CompositeMessageParameter;

import javax.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ConstraintValidationException extends ApplicationException {
    private Set<ConstraintViolation<?>> violations;

    public ConstraintValidationException(String message, Set<ConstraintViolation<?>> violations) {
        super(message);
        // сохраняем порядок
        this.violations = new LinkedHashSet<>(violations);
    }

    public Set<ConstraintViolation<?>> getViolations() {
        return violations;
    }

    @Override
    public Object[] getParameters() {
        return new Object[] {CompositeMessageParameter.generateWithMessages(convert(violations))};
    }

    private static List<String> convert(Set<ConstraintViolation<?>> violations) {
        List<String> messages = new ArrayList<>();
        for (ConstraintViolation<?> violation : violations) {
            messages.add(violation.getMessage());
        }
        return messages;
    }
}
