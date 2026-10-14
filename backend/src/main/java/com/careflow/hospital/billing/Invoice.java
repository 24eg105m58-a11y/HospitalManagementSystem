package com.careflow.hospital.billing;

import com.careflow.hospital.patient.Patient;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) private String invoiceNumber;
    @ManyToOne(optional = false) @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private LocalDate invoiceDate;
    @Column(nullable = false) private String status;

    protected Invoice() {}

    public Invoice(String invoiceNumber, Patient patient, BigDecimal amount, LocalDate invoiceDate, String status) {
        this.invoiceNumber = invoiceNumber;
        this.patient = patient;
        this.amount = amount;
        this.invoiceDate = invoiceDate;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public Patient getPatient() { return patient; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public String getStatus() { return status; }
}
