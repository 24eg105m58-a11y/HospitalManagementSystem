package com.careflow.hospital.auth;

import com.careflow.hospital.patient.Patient;
import com.careflow.hospital.patient.PatientRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {
    private final HospitalUserRepository users;
    private final PatientRepository patients;
    private final PasswordEncoder passwords;
    private final JwtService tokens;

    public AuthService(HospitalUserRepository users, PatientRepository patients, PasswordEncoder passwords, JwtService tokens) {
        this.users = users;
        this.patients = patients;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    public AuthResponse login(AuthRequest request) {
        HospitalUser user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(candidate -> passwords.matches(request.password(), candidate.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return response(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        HospitalUser user = users.save(new HospitalUser(email, passwords.encode(request.password()), request.fullName().trim(), HospitalRole.PATIENT));
        Patient patient = new Patient(request.fullName().trim(), email, null, null, null, null);
        patient.setUser(user);
        patients.save(patient);
        return response(user);
    }

    private AuthResponse response(HospitalUser user) {
        return new AuthResponse(tokens.createToken(user), AuthenticatedUser.from(user));
    }
}
