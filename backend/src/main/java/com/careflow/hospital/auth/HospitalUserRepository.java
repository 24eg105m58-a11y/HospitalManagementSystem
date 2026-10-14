package com.careflow.hospital.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HospitalUserRepository extends JpaRepository<HospitalUser, Long> {
    Optional<HospitalUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
