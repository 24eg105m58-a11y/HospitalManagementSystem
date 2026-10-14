package com.careflow.hospital.auth;

public record AuthenticatedUser(Long id, String email, String fullName, HospitalRole role) {
    public static AuthenticatedUser from(HospitalUser user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
