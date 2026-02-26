package com.biolab.common.validation;

import com.biolab.common.security.SecurityConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates passwords against the BioLab security policy.
 *
 * <h3>Rules (Slide 10 — Complete Security Controls):</h3>
 * <ul>
 *   <li>Minimum 8 characters</li>
 *   <li>At least one uppercase letter (A-Z)</li>
 *   <li>At least one lowercase letter (a-z)</li>
 *   <li>At least one digit (0-9)</li>
 *   <li>At least one special character</li>
 *   <li>No more than 3 consecutive identical characters</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
public class PasswordPolicyValidator implements ConstraintValidator<PasswordPolicy, String> {

    private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,./<>?].*");
    private static final Pattern REPEATED = Pattern.compile(".*(.)\\1{2,}.*");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return false;
        }

        List<String> violations = new ArrayList<>();

        if (password.length() < SecurityConstants.PASSWORD_MIN_LENGTH) {
            violations.add("Must be at least " + SecurityConstants.PASSWORD_MIN_LENGTH + " characters");
        }
        if (!UPPERCASE.matcher(password).matches()) {
            violations.add("Must contain at least one uppercase letter");
        }
        if (!LOWERCASE.matcher(password).matches()) {
            violations.add("Must contain at least one lowercase letter");
        }
        if (!DIGIT.matcher(password).matches()) {
            violations.add("Must contain at least one digit");
        }
        if (!SPECIAL.matcher(password).matches()) {
            violations.add("Must contain at least one special character");
        }
        if (REPEATED.matcher(password).matches()) {
            violations.add("Must not contain 3 or more consecutive identical characters");
        }

        if (!violations.isEmpty()) {
            context.disableDefaultConstraintViolation();
            violations.forEach(v ->
                    context.buildConstraintViolationWithTemplate(v).addConstraintViolation());
            return false;
        }
        return true;
    }
}
