# Backend

Spring Boot backend for the medical data platform.

## Responsibilities

- exposes REST endpoints for frontend data access
- imports normalized CSV datasets into PostgreSQL
- serves patients, vital signs, predictions, timeline, consult notes, and medications
- integrates with the Python AI service

## Runtime

Default local backend URL:

- `http://localhost:8081`

Default local database configuration:

- JDBC URL: `jdbc:postgresql://localhost:5433/medical_backend_full`
- username: `postgres`
- password: `postgres`

AI service base URL:

- `http://localhost:8001`

These values are defined in:

- [application.properties](/C:/Users/mauri/Projects/medical-data-platform/apps/backend/src/main/resources/application.properties)

## Start locally

From `apps/backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

Run tests:

```powershell
.\mvnw.cmd test
```

## Flyway

The backend uses Flyway migrations and validates the schema through JPA.

Important notes:

- the app expects UUID-based tables
- if you point it at an older database with bigint ids, startup validation will fail
- the active local database is `medical_backend_full`

## Dataset import

The import endpoint is:

- `POST /api/dataset-imports`

Supported dataset type:

- `NORMALIZED_MEDICAL_DATA`

Expected import directory contents:

- `patient.csv`
- `patient_address.csv`
- `vital_signs.csv`
- optional `patient_features.csv`
- optional `import_summary.csv`

The current working import directory is:

- `C:\Users\mauri\Projects\medical-data-platform\apps\ai-service\data\exports\backend-import`

Example import request:

```powershell
$body = @{
  sourceName = 'demo-local-dutch'
  datasetType = 'NORMALIZED_MEDICAL_DATA'
  sourceDirectoryPath = 'C:\Users\mauri\Projects\medical-data-platform\apps\ai-service\data\exports\backend-import'
  notes = 'Generated via ai-service notebook pipeline'
  replaceExistingData = $true
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8081/api/dataset-imports' `
  -ContentType 'application/json' `
  -Body $body
```

## Frontend-facing endpoints

The frontend currently depends on:

- `GET /api/patients/search?q=...`
- `GET /api/patients/{patientId}`
- `GET /api/patients/{patientId}/vitals/latest`
- `GET /api/patients/{patientId}/timeline`
- `GET /api/patients/{patientId}/predictions/latest`
- `GET /api/patients/{patientId}/consult-notes`
- `GET /api/patients/{patientId}/medications`

## Current loaded scope

After importing the demo normalized dataset, these endpoints return real data:

- patient search
- patient detail
- latest vitals
- timeline
- predictions
- consult notes
- medications

The demo import scope is intentionally limited to 25 patients and now seeds historical consult notes plus current and past medications for those imported patients.
