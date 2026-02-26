package com.biolab.notification.dto;

public record NotificationPreferenceDto(boolean emailEnabled, boolean projectUpdates,
    boolean newMessages, boolean invoiceReminders, boolean securityAlerts, boolean marketing) {}
