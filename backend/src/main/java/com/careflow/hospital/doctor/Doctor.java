package com.careflow.hospital.doctor;

import com.careflow.hospital.auth.HospitalUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "doctors")
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String fullName;

    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String specialization;

    private String phone;

    @Column(nullable = false)
    private String status = "Active";

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private HospitalUser user;

    protected Doctor() {}

    public Doctor(String fullName, String email, String specialization, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.specialization = specialization;
        this.phone = phone;
    }

    public void setUser(HospitalUser user) { this.user = user; }
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getSpecialization() { return specialization; }
    public String getPhone() { return phone; }
    public String getStatus() { return status; }
}
