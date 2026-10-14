package com.careflow.hospital.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    java.util.Optional<Patient> findByEmailIgnoreCase(String email);
    List<Patient> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
    Optional<Patient> findByUser_EmailIgnoreCase(String email);
}
