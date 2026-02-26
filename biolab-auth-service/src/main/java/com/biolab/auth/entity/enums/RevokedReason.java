package com.biolab.auth.entity.enums;

/** 
 * Reason a refresh token was revoked.
 * 
 * <ul>
 *   <li>ROTATED - Token was replaced during normal rotation</li>
 *   <li>LOGOUT - User logged out</li>
 *   <li>REUSE_DETECTED - Potential token theft detected</li>
 *   <li>ADMIN_REVOKED - Administrator forced logout</li>
 *   <li>PASSWORD_CHANGE - User changed their password</li>
 *   <li>EXPIRED_CLEANUP - Scheduled cleanup of expired tokens</li>
 * </ul>
 */
public enum RevokedReason { 
    ROTATED, 
    LOGOUT, 
    REUSE_DETECTED, 
    ADMIN_REVOKED, 
    PASSWORD_CHANGE, 
    EXPIRED_CLEANUP 
}
