package org.fen.fen.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.fen.fen.validation.annotation.ValidCRF;

import java.util.regex.Pattern;

public class ValidCRFValidator  implements ConstraintValidator<ValidCRF, String> {

    private final Pattern CRF_PATTERN = Pattern.compile("^\\d{5,6}-\\d{1}$");

    @Override
    public void initialize(ValidCRF constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String crf, ConstraintValidatorContext context) {
        if (Strings.isBlank(crf)) {
            return false;
        }

        return CRF_PATTERN.matcher(crf).matches();
    }
}
