package com.careflow.hospital.record;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    @Override
    @EntityGraph(attributePaths = {"patient", "doctor"})
    List<MedicalRecord> findAll();
}
