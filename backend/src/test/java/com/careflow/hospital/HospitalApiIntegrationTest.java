package com.careflow.hospital;

import com.careflow.hospital.auth.AuthRequest;
import com.careflow.hospital.auth.AuthResponse;
import com.careflow.hospital.auth.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:careflowtest;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
class HospitalApiIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void seededAccountsCollectionsPatientScopeAndRegistrationWork() {
        ResponseEntity<JsonNode> health = rest.getForEntity(url("/actuator/health"), JsonNode.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody().path("status").asText()).isEqualTo("UP");

        AuthResponse admin = login("admin@mail.com");
        assertThat(admin.user().role().name()).isEqualTo("ADMIN");

        assertCollectionCount("/api/patients", admin.token(), 26);
        assertCollectionCount("/api/doctors", admin.token(), 26);
        assertCollectionCount("/api/appointments", admin.token(), 26);
        assertCollectionCount("/api/medical-records", admin.token(), 26);
        assertCollectionCount("/api/prescriptions", admin.token(), 26);
        assertCollectionCount("/api/invoices", admin.token(), 26);

        AuthResponse doctor = login("doctor@mail.com");
        assertThat(doctor.user().role().name()).isEqualTo("DOCTOR");
        assertThat(get("/api/medical-records", doctor.token()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/invoices", doctor.token()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        AuthResponse receptionist = login("receptionist@mail.com");
        assertThat(receptionist.user().role().name()).isEqualTo("RECEPTIONIST");
        assertThat(get("/api/invoices", receptionist.token()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/medical-records", receptionist.token()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        AuthResponse patient = login("patient@mail.com");
        assertThat(get("/api/patients", patient.token()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<JsonNode> ownProfile = get("/api/patients/me", patient.token());
        assertThat(ownProfile.getStatusCode()).isEqualTo(HttpStatus.OK);
        String ownEmail = ownProfile.getBody().path("email").asText();

        ResponseEntity<List<JsonNode>> ownAppointments = rest.exchange(
                url("/api/appointments"),
                HttpMethod.GET,
                authorized(patient.token()),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(ownAppointments.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownAppointments.getBody()).isNotEmpty();
        assertThat(ownAppointments.getBody())
                .allSatisfy(appointment -> assertThat(appointment.path("patient").path("email").asText())
                        .isEqualTo(ownEmail));

        String appointmentRequest = """
                {
                  "patientId": 1,
                  "doctorId": 1,
                  "department": "Cardiology",
                  "appointmentDate": "%s",
                  "appointmentTime": "%s"
                }
                """.formatted(LocalDate.now().plusDays(14), LocalTime.of(11, 0));
        ResponseEntity<JsonNode> bookedAppointment = rest.exchange(
                url("/api/appointments"),
                HttpMethod.POST,
                new HttpEntity<>(appointmentRequest, jsonHeaders(patient.token())),
                JsonNode.class
        );
        assertThat(bookedAppointment.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bookedAppointment.getBody().path("patient").path("email").asText()).isEqualTo(ownEmail);

        AuthResponse registered = rest.postForObject(
                url("/api/auth/register"),
                new RegisterRequest("Taylor Morgan", "taylor@mail.com", "abcdef"),
                AuthResponse.class
        );
        assertThat(registered).isNotNull();
        assertThat(registered.user().role().name()).isEqualTo("PATIENT");
        assertThat(get("/api/patients/me", registered.token()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private AuthResponse login(String email) {
        AuthResponse response = rest.postForObject(
                url("/api/auth/login"),
                new AuthRequest(email, "123456"),
                AuthResponse.class
        );
        assertThat(response).isNotNull();
        return response;
    }

    private void assertCollectionCount(String path, String token, int expected) {
        ResponseEntity<List<JsonNode>> response = rest.exchange(
                url(path),
                HttpMethod.GET,
                authorized(token),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(expected);
    }

    private ResponseEntity<JsonNode> get(String path, String token) {
        return rest.exchange(url(path), HttpMethod.GET, authorized(token), JsonNode.class);
    }

    private HttpEntity<Void> authorized(String token) {
        return new HttpEntity<>(jsonHeaders(token));
    }

    private HttpHeaders jsonHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
