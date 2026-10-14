package com.careflow.hospital.patient;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientRepository patients;
    public PatientController(PatientRepository patients) { this.patients = patients; }

    @GetMapping public List<Patient> list(@RequestParam(required = false) String search) {
        return search == null || search.isBlank() ? patients.findAll() : patients.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search);
    }
    @GetMapping("/me") public Patient getCurrentPatient(Authentication authentication) {
        return patients.findByUser_EmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient profile not found"));
    }
    @GetMapping("/{id}") public Patient get(@PathVariable Long id) {
        return patients.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Patient create(@Valid @RequestBody Patient patient) { return patients.save(patient); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) {
        if (!patients.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        patients.deleteById(id);
    }
}
