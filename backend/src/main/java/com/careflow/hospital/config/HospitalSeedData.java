package com.careflow.hospital.config;

import com.careflow.hospital.appointment.Appointment;
import com.careflow.hospital.appointment.AppointmentRepository;
import com.careflow.hospital.auth.HospitalRole;
import com.careflow.hospital.auth.HospitalUser;
import com.careflow.hospital.auth.HospitalUserRepository;
import com.careflow.hospital.billing.Invoice;
import com.careflow.hospital.billing.InvoiceRepository;
import com.careflow.hospital.doctor.Doctor;
import com.careflow.hospital.doctor.DoctorRepository;
import com.careflow.hospital.patient.Patient;
import com.careflow.hospital.patient.PatientRepository;
import com.careflow.hospital.prescription.Prescription;
import com.careflow.hospital.prescription.PrescriptionRepository;
import com.careflow.hospital.record.MedicalRecord;
import com.careflow.hospital.record.MedicalRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HospitalSeedData implements ApplicationRunner {
    private final ObjectMapper json;
    private final HospitalUserRepository users;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final MedicalRecordRepository records;
    private final PrescriptionRepository prescriptions;
    private final InvoiceRepository invoices;
    private final PasswordEncoder passwordEncoder;
    private final String initialPassword;

    public HospitalSeedData(ObjectMapper json, HospitalUserRepository users, PatientRepository patients,
                            DoctorRepository doctors, AppointmentRepository appointments,
                            MedicalRecordRepository records, PrescriptionRepository prescriptions,
                            InvoiceRepository invoices, PasswordEncoder passwordEncoder,
                            @Value("${app.seed.default-password}") String initialPassword) {
        this.json = json;
        this.users = users;
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.records = records;
        this.prescriptions = prescriptions;
        this.invoices = invoices;
        this.passwordEncoder = passwordEncoder;
        this.initialPassword = initialPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsers();
        Map<String, Patient> patientByEmail = seedPatients();
        Map<String, Doctor> doctorByEmail = seedDoctors();
        seedAppointments(patientByEmail, doctorByEmail);
        seedMedicalRecords(patientByEmail, doctorByEmail);
        seedPrescriptions(patientByEmail, doctorByEmail);
        seedInvoices(patientByEmail);
    }

    private void seedUsers() {
        for (JsonNode row : rows("users.json")) {
            String email = text(row, "email");
            if (!users.existsByEmailIgnoreCase(email)) {
                HospitalUser user = new HospitalUser(email, passwordEncoder.encode(initialPassword),
                        text(row, "fullName"), HospitalRole.valueOf(text(row, "role")));
                users.save(user);
            }
        }
    }

    private Map<String, Patient> seedPatients() {
        Map<String, Patient> byEmail = new HashMap<>();
        for (Patient patient : patients.findAll()) {
            if (patient.getEmail() != null) {
                byEmail.put(patient.getEmail().toLowerCase(java.util.Locale.ROOT), patient);
            }
        }
        for (JsonNode row : rows("patients.json")) {
            String email = text(row, "email");
            String normalizedEmail = email.toLowerCase(java.util.Locale.ROOT);
            if (!byEmail.containsKey(normalizedEmail)) {
                HospitalUser user = users.findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new IllegalStateException("Missing seed user for patient: " + email));
                Patient patient = new Patient(user.getFullName(), email, text(row, "phone"),
                        LocalDate.parse(text(row, "dateOfBirth")), text(row, "bloodGroup"), text(row, "gender"));
                patient.setUser(user);
                byEmail.put(normalizedEmail, patients.save(patient));
            }
        }
        return byEmail;
    }

    private Map<String, Doctor> seedDoctors() {
        Map<String, Doctor> byEmail = new HashMap<>();
        for (Doctor doctor : doctors.findAll()) {
            if (doctor.getEmail() != null) {
                byEmail.put(doctor.getEmail().toLowerCase(java.util.Locale.ROOT), doctor);
            }
        }
        for (JsonNode row : rows("doctors.json")) {
            String email = text(row, "email");
            String normalizedEmail = email.toLowerCase(java.util.Locale.ROOT);
            if (!byEmail.containsKey(normalizedEmail)) {
                HospitalUser user = users.findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new IllegalStateException("Missing seed user for doctor: " + email));
                Doctor doctor = new Doctor(user.getFullName(), email, text(row, "specialization"), text(row, "phone"));
                doctor.setUser(user);
                byEmail.put(normalizedEmail, doctors.save(doctor));
            }
        }
        return byEmail;
    }

    private void seedAppointments(Map<String, Patient> patientByEmail, Map<String, Doctor> doctorByEmail) {
        Set<String> existing = new HashSet<>();
        for (Appointment appointment : appointments.findAll()) {
            existing.add(key(appointment.getPatient().getEmail(), appointment.getDoctor().getEmail(),
                    appointment.getDepartment(), appointment.getAppointmentDate().toString(),
                    appointment.getAppointmentTime().toString()));
        }
        for (JsonNode row : rows("appointments.json")) {
            Patient patient = required(patientByEmail, row, "patientEmail", "patient");
            Doctor doctor = required(doctorByEmail, row, "doctorEmail", "doctor");
            String department = text(row, "department");
            LocalDate date = LocalDate.parse(text(row, "appointmentDate"));
            LocalTime time = LocalTime.parse(text(row, "appointmentTime"));
            if (existing.add(key(patient.getEmail(), doctor.getEmail(), department, date.toString(), time.toString()))) {
                appointments.save(new Appointment(patient, doctor, department, date, time, text(row, "status")));
            }
        }
    }

    private void seedMedicalRecords(Map<String, Patient> patientByEmail, Map<String, Doctor> doctorByEmail) {
        Set<String> existing = new HashSet<>();
        for (MedicalRecord record : records.findAll()) {
            existing.add(key(record.getPatient().getEmail(), record.getDoctor().getEmail(),
                    record.getVisitDate().toString(), record.getDiagnosis()));
        }
        for (JsonNode row : rows("medical-records.json")) {
            Patient patient = required(patientByEmail, row, "patientEmail", "patient");
            Doctor doctor = required(doctorByEmail, row, "doctorEmail", "doctor");
            LocalDate date = LocalDate.parse(text(row, "visitDate"));
            String diagnosis = text(row, "diagnosis");
            if (existing.add(key(patient.getEmail(), doctor.getEmail(), date.toString(), diagnosis))) {
                records.save(new MedicalRecord(patient, doctor, date, diagnosis, text(row, "notes")));
            }
        }
    }

    private void seedPrescriptions(Map<String, Patient> patientByEmail, Map<String, Doctor> doctorByEmail) {
        Set<String> existing = new HashSet<>();
        for (Prescription prescription : prescriptions.findAll()) {
            existing.add(key(prescription.getPatient().getEmail(), prescription.getDoctor().getEmail(),
                    prescription.getPrescribedDate().toString(), prescription.getMedication(), prescription.getDosage()));
        }
        for (JsonNode row : rows("prescriptions.json")) {
            Patient patient = required(patientByEmail, row, "patientEmail", "patient");
            Doctor doctor = required(doctorByEmail, row, "doctorEmail", "doctor");
            LocalDate date = LocalDate.parse(text(row, "prescribedDate"));
            String medication = text(row, "medication");
            String dosage = text(row, "dosage");
            if (existing.add(key(patient.getEmail(), doctor.getEmail(), date.toString(), medication, dosage))) {
                prescriptions.save(new Prescription(patient, doctor, date, medication, dosage, text(row, "status")));
            }
        }
    }

    private void seedInvoices(Map<String, Patient> patientByEmail) {
        Set<String> existing = new HashSet<>();
        for (Invoice invoice : invoices.findAll()) {
            existing.add(invoice.getInvoiceNumber());
        }
        for (JsonNode row : rows("invoices.json")) {
            String invoiceNumber = text(row, "invoiceNumber");
            if (existing.add(invoiceNumber)) {
                invoices.save(new Invoice(invoiceNumber, required(patientByEmail, row, "patientEmail", "patient"),
                        new BigDecimal(text(row, "amount")), LocalDate.parse(text(row, "invoiceDate")), text(row, "status")));
            }
        }
    }

    private String key(String... parts) {
        return String.join("|", parts).toLowerCase(java.util.Locale.ROOT);
    }

    private <T> T required(Map<String, T> entities, JsonNode row, String reference, String collection) {
        String email = text(row, reference).toLowerCase(java.util.Locale.ROOT);
        T entity = entities.get(email);
        if (entity == null) {
            throw new IllegalStateException("Seed " + collection + " not found for " + email);
        }
        return entity;
    }

    private String text(JsonNode row, String field) {
        JsonNode value = row.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalStateException("Invalid " + field + " in seed-data collection");
        }
        return value.asText();
    }

    private JsonNode rows(String filename) {
        try (InputStream stream = HospitalSeedData.class.getResourceAsStream("/seed-data/" + filename)) {
            if (stream == null) {
                throw new IllegalStateException("Missing required seed-data/" + filename);
            }
            JsonNode data = json.readTree(stream);
            if (data == null || !data.isArray() || data.isEmpty()) {
                throw new IllegalStateException("Seed-data/" + filename + " must contain a nonempty JSON array");
            }
            return data;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load seed-data/" + filename, exception);
        }
    }
}
