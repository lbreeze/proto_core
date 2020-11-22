package ru.v6.mark.prototype.domain.constant;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface HasProtocol {

    public ProtocolIdentifier identifier() default ProtocolIdentifier.RESULT;
    public boolean now() default true;
    public boolean userOnly() default false;
    public String action() default "";

    public String linkPrefix () default "";

    public boolean onlyIfChangedAtributes() default false;
}
