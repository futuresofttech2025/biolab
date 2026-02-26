package com.biolab.auth.entity.enums;

/** Authentication event actions for audit log. */
public enum LoginAction { LOGIN, LOGOUT, FAILED_LOGIN, TOKEN_REFRESH, PASSWORD_RESET, MFA_CHALLENGE, TOKEN_ROTATION, REUSE_DETECTED }
