package ru.v6.mark.prototype.domain.constant;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ProtocolAttribute {

    public String value() default "";
}
