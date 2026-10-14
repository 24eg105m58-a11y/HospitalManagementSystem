package com.careflow.hospital.prescription;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {
    private final PrescriptionRepository prescriptions;

    public PrescriptionController(PrescriptionRepository prescriptions) {
        this.prescriptions = prescriptions;
    }

    @GetMapping
    public List<Prescription> list() {
        return prescriptions.findAll();
    }
}
