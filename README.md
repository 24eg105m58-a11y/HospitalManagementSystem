# Careflow Hospital Management

Careflow is a full-stack hospital management application with a public landing
page, patient registration, role-based login, a React + Vite frontend, a Java
21 / Spring Boot REST API, and a MySQL database.

## Run locally

Start MySQL 8 and configure `backend/.env` using `backend/.env.example`:

```powershell
docker compose up -d database
```

In one terminal, start the Spring Boot API:

```powershell
cd backend
mvn spring-boot:run
```

In a second terminal, start the frontend:

```powershell
cd frontend
npm install
npm run dev
```

Open the URL printed by Vite, usually `http://localhost:5173`. The public home
page provides the registration and sign-in flows. The frontend forwards
`/api` requests to the API at `http://localhost:8080`.

## Verify the application

Run the backend integration test from the project root:

```powershell
mvn -f backend/pom.xml test
```

The test uses an in-memory H2 database and checks health, starter-account
login, seeded collection counts, role-based access, patient appointment
privacy, and patient registration. It does not require a running MySQL server
or modify your local database.

## Local starter accounts

The local database is automatically populated on first startup with 20 records
in each of the patient, doctor, appointment, medical-record, prescription, and
invoice collections. Its 42 role-specific starter accounts all use the local
development password `123456`.

| Role | Starter email | Password |
| --- | --- | --- |
| Administrator | `admin@mail.com` | `123456` |
| Doctor | `doctor@mail.com` | `123456` |
| Receptionist | `receptionist@mail.com` | `123456` |
| Patient | `patient@mail.com` | `123456` |

Additional doctor and patient starter accounts are listed in
`backend/src/main/resources/seed-data/users.json`. Passwords are hashed with
BCrypt before they are saved in the database. The seed password is configurable
using `SEED_DEFAULT_PASSWORD`.

All bundled patient, appointment, medical-record, and billing information is
**synthetic local testing data**; it is not real patient information or medical
guidance. Collection JSON files live in
`backend/src/main/resources/seed-data/`. On startup, the backend only seeds
each collection if that database table is empty.

### Production security

The shared `123456` starter password and default JWT signing key are for local
development only. Before deploying, set a unique long `JWT_SECRET`, set
`SEED_DATA_ENABLED=false`, and supply unique credentials and database secrets
through your deployment environment. Never use the starter accounts with
real patient information. New public registrations are limited to the patient
role. Passwords need at least six characters; there are no special-character
or regular-expression rules. Patient passwords are never returned by the API.

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Create a patient account and profile |
| `POST` | `/api/auth/login` | Authenticate and receive a signed JWT |
| `GET` | `/api/patients` | List/search patients; staff only |
| `GET` | `/api/patients/me` | Retrieve the signed-in patient's profile |
| `POST` | `/api/patients` | Register a patient; staff only |
| `GET` | `/api/doctors` | Browse the doctor directory |
| `POST` | `/api/doctors` | Add a doctor; administrators only |
| `GET`, `POST` | `/api/appointments` | List or book appointments |
| `DELETE` | `/api/appointments/{id}` | Delete an appointment; staff only |
| `GET` | `/api/medical-records` | Read medical records; administrators and doctors |
| `GET` | `/api/prescriptions` | Read prescriptions; administrators and doctors |
| `GET` | `/api/invoices` | Read invoices; administrators and receptionists |
| `POST` | `/api/assistant/ask` | Ask the approved-information assistant |
| `GET` | `/actuator/health` | Service health |

Except for registration, login, and the health endpoint, API requests require
`Authorization: Bearer <token>`.

Registration request:

```json
{"fullName":"Taylor Morgan","email":"taylor@mail.com","password":"simplepass"}
```

Appointment request (staff provide `patientId`; patients always book for
themselves):

```json
{"patientId":1,"doctorId":1,"department":"Cardiology","appointmentDate":"2026-10-20","appointmentTime":"11:00"}
```

## Docker

Configure `backend/.env` before launching the complete application from the
project root:

```powershell
docker compose up --build
```

The database is available on port `3306`, the API on port `8080`, and the
frontend on port `80`.

## Current scope

Authentication, patient and doctor profiles, appointments, medical-record,
prescription, and invoice persistence are connected to the database and access
controlled by role. The assistant remains a safe placeholder until an approved
knowledge base and provider are configured. The application and synthetic
starter data are intended for development, not clinical production use.
