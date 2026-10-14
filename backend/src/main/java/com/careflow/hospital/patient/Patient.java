package com.careflow.hospital.patient;

import com.careflow.hospital.auth.HospitalUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
public class Patient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank private String fullName;
    @Email @Column(unique = true) private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String bloodGroup;
    private String gender;
    @Column(nullable = false)
    private String status = "Active";

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private HospitalUser user;

    protected Patient() {}
    public Patient(String fullName, String email, String phone, LocalDate dateOfBirth, String bloodGroup, String gender) {
        this.fullName = fullName; this.email = email; this.phone = phone;
        this.dateOfBirth = dateOfBirth; this.bloodGroup = bloodGroup; this.gender = gender;
    }
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getBloodGroup() { return bloodGroup; }
    public String getGender() { return gender; }
    public String getStatus() { return status; }
    public HospitalUser getUser() { return user; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public void setGender(String gender) { this.gender = gender; }
    public void setUser(HospitalUser user) { this.user = user; }
}
