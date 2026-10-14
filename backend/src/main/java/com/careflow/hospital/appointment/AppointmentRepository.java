package com.careflow.hospital.appointment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @EntityGraph(attributePaths = {"patient", "doctor"})
    List<Appointment> findByAppointmentDateOrderByAppointmentTimeAsc(LocalDate appointmentDate);
    @EntityGraph(attributePaths = {"patient", "doctor"})
    List<Appointment> findAllByOrderByAppointmentDateAscAppointmentTimeAsc();
    @EntityGraph(attributePaths = {"patient", "doctor"})
    List<Appointment> findByPatient_User_EmailIgnoreCaseOrderByAppointmentDateAscAppointmentTimeAsc(String email);
}
