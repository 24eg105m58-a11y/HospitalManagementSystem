package com.careflow.hospital.prescription;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    @Override
    @EntityGraph(attributePaths = {"patient", "doctor"})
    List<Prescription> findAll();
}
