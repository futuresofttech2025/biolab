package com.biolab.auth.service;

import com.biolab.auth.dto.request.*;
import com.biolab.auth.dto.response.*;

/**
 * Core authentication service interface.
 * Handles login, registration, JWT token issuance with refresh token rotation,
 * MFA, password management, and token validation.
 *
 * @author BioLab Engineering Team
 */
public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);
    AuthResponse verifyMfa(MfaVerifyRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent);
    void logout(String accessToken, String refreshToken);
    MessageResponse forgotPassword(ForgotPasswordRequest request);
    MessageResponse resetPassword(ResetPasswordRequest request);
    MessageResponse changePassword(String userId, ChangePasswordRequest request);
    TokenValidationResponse validateToken(String token);

    /** Verify email address using the token from the verification link. */
    MessageResponse verifyEmail(String rawToken);

    /** Resend verification email — rate-limited to once per minute per user. */
    MessageResponse resendVerificationEmail(String email);
}