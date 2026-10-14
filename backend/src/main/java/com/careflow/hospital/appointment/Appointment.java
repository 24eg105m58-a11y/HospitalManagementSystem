package com.careflow.hospital.appointment;

import com.careflow.hospital.doctor.Doctor;
import com.careflow.hospital.patient.Patient;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime appointmentTime;

    @Column(nullable = false)
    private String status = "CONFIRMED";

    protected Appointment() {}

    public Appointment(Patient patient, Doctor doctor, String department, LocalDate appointmentDate,
                       LocalTime appointmentTime, String status) {
        this.patient = patient;
        this.doctor = doctor;
        this.department = department;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.status = status;
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public String getPatientName() { return patient.getFullName(); }
    public String getDoctorName() { return doctor.getFullName(); }
    public String getDepartment() { return department; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public String getStatus() { return status; }
}
