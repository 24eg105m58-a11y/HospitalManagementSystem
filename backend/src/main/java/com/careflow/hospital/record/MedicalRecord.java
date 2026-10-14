package com.careflow.hospital.record;

import com.careflow.hospital.doctor.Doctor;
import com.careflow.hospital.patient.Patient;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "medical_records")
public class MedicalRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(optional = false) @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false) private LocalDate visitDate;
    @Column(nullable = false) private String diagnosis;
    @Column(length = 2000) private String notes;

    protected MedicalRecord() {}

    public MedicalRecord(Patient patient, Doctor doctor, LocalDate visitDate, String diagnosis, String notes) {
        this.patient = patient;
        this.doctor = doctor;
        this.visitDate = visitDate;
        this.diagnosis = diagnosis;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public LocalDate getVisitDate() { return visitDate; }
    public String getDiagnosis() { return diagnosis; }
    public String getNotes() { return notes; }
}
