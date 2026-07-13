package org.fen.fen.validation.annotation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.fen.fen.validation.ValidCRFValidator;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCRFValidator.class)
@Documented
public @interface ValidCRF {

    String message() default "O CRF informado é invalido!";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
