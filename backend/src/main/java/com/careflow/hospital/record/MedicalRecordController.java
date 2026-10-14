package com.careflow.hospital.record;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {
    private final MedicalRecordRepository records;

    public MedicalRecordController(MedicalRecordRepository records) {
        this.records = records;
    }

    @GetMapping
    public List<MedicalRecord> list() {
        return records.findAll();
    }
}
