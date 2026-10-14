package com.careflow.hospital.prescription;

import com.careflow.hospital.doctor.Doctor;
import com.careflow.hospital.patient.Patient;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "prescriptions")
public class Prescription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(optional = false) @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false) private LocalDate prescribedDate;
    @Column(nullable = false) private String medication;
    @Column(nullable = false) private String dosage;
    @Column(nullable = false) private String status;

    protected Prescription() {}

    public Prescription(Patient patient, Doctor doctor, LocalDate prescribedDate, String medication, String dosage, String status) {
        this.patient = patient;
        this.doctor = doctor;
        this.prescribedDate = prescribedDate;
        this.medication = medication;
        this.dosage = dosage;
        this.status = status;
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public LocalDate getPrescribedDate() { return prescribedDate; }
    public String getMedication() { return medication; }
    public String getDosage() { return dosage; }
    public String getStatus() { return status; }
}
