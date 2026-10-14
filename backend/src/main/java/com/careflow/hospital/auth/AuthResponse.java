package com.careflow.hospital.auth;

public record AuthResponse(String token, AuthenticatedUser user) {}
