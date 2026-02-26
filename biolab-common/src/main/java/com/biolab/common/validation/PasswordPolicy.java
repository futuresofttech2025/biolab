package com.biolab.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Annotation for server-side password strength validation.
 *
 * <p>Enforces the BioLab password policy (Slide 10):</p>
 * <ul>
 *   <li>Minimum 8 characters (12 recommended)</li>
 *   <li>At least one uppercase letter</li>
 *   <li>At least one lowercase letter</li>
 *   <li>At least one digit</li>
 *   <li>At least one special character (!&#64;#$%^&amp;*()_+-=[]{}|;:,./&lt;&gt;?)</li>
 * </ul>
 *
 * <pre>
 *   &#64;PasswordPolicy
 *   private String password;
 * </pre>
 *
 * @author BioLab Engineering Team
 */
@Documented
@Constraint(validatedBy = PasswordPolicyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordPolicy {
    String message() default "Password does not meet security requirements";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
