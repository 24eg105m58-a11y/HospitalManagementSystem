package com.careflow.hospital.appointment;

import com.careflow.hospital.auth.HospitalRole;
import com.careflow.hospital.doctor.DoctorRepository;
import com.careflow.hospital.patient.Patient;
import com.careflow.hospital.patient.PatientRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final DoctorRepository doctors;

    public AppointmentController(AppointmentRepository appointments, PatientRepository patients, DoctorRepository doctors) {
        this.appointments = appointments;
        this.patients = patients;
        this.doctors = doctors;
    }

    @GetMapping
    public List<Appointment> list(@RequestParam(required = false) LocalDate date, Authentication authentication) {
        if (isPatient(authentication)) {
            return appointments.findByPatient_User_EmailIgnoreCaseOrderByAppointmentDateAscAppointmentTimeAsc(authentication.getName());
        }
        return date == null
                ? appointments.findAllByOrderByAppointmentDateAscAppointmentTimeAsc()
                : appointments.findByAppointmentDateOrderByAppointmentTimeAsc(date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Appointment create(@Valid @RequestBody CreateAppointmentRequest request, Authentication authentication) {
        Patient patient;
        if (isPatient(authentication)) {
            patient = patients.findByUser_EmailIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient profile not found"));
        } else {
            patient = patients.findById(request.patientId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        }
        var doctor = doctors.findById(request.doctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        return appointments.save(new Appointment(patient, doctor, request.department(), request.appointmentDate(),
                request.appointmentTime(), "CONFIRMED"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!appointments.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
        }
        appointments.deleteById(id);
    }

    private boolean isPatient(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_" + HospitalRole.PATIENT.name()));
    }

    public record CreateAppointmentRequest(
            @NotNull Long patientId,
            @NotNull Long doctorId,
            @NotBlank String department,
            @NotNull LocalDate appointmentDate,
            @NotNull LocalTime appointmentTime
    ) {}
}
