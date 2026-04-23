# Medical Data Platform

Monorepo for a medical workflow platform with:

- Angular frontend
- Spring Boot backend
- Python AI service
- PostgreSQL via Docker

The active local setup uses the full normalized dataset, imported into PostgreSQL and served to the frontend through the Spring backend.

## Monorepo structure

- `apps/frontend` - Angular UI for patient search and patient detail
- `apps/backend` - Spring Boot REST API and CSV import backend
- `apps/ai-service` - Python AI service, notebooks, dataset pipeline, export pipeline
- `database` - database-related assets
- `infra` - Docker and infrastructure files
- `docs` - project documentation

## Current scope

The imported full dataset currently fills:

- patients
- patient addresses
- vital signs

These sections still depend on later imports or app-created data:

- timeline events
- predictions
- consult notes
- medications

## End-to-end local flow

1. Start PostgreSQL with Docker.
2. Run the notebook export pipeline in `apps/ai-service`.
3. Start the Spring Boot backend.
4. Import the generated normalized dataset through the backend API.
5. Start the Angular frontend.
6. Open the UI and search for patients.

## Quick start

### 1. Start PostgreSQL

The local setup expects PostgreSQL on:

- host: `localhost`
- port: `5433`
- username: `postgres`
- password: `postgres`

The backend is configured to use:

- database: `medical_backend_full`

### 2. Generate normalized import files from the full dataset

From `apps/ai-service`:

```powershell
venv\Scripts\python.exe scripts\run_notebook_pipeline.py --source full --export-name full
```

This command:

- stages raw CSV files from `apps/ai-service/datasets`
- executes notebooks `01` through `05`
- writes processed notebook outputs under `data/`
- writes backend-ready import files to `data/exports/backend-import-full`

Generated backend import files:

- `apps/ai-service/data/exports/backend-import-full/patient.csv`
- `apps/ai-service/data/exports/backend-import-full/patient_address.csv`
- `apps/ai-service/data/exports/backend-import-full/vital_signs.csv`
- `apps/ai-service/data/exports/backend-import-full/patient_features.csv`
- `apps/ai-service/data/exports/backend-import-full/import_summary.csv`

### 3. Start the stack

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-full.ps1
```

URLs:

- backend: `http://localhost:8081`
- frontend: `http://localhost:4200`

### 4. Import the generated dataset

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File apps\backend\scripts\import-dataset.ps1 `
  -BackendUrl 'http://localhost:8081' `
  -SourceName 'full-local-dutch' `
  -SourceDirectoryPath 'C:\Users\mauri\Projects\medical-data-platform\apps\ai-service\data\exports\backend-import-full' `
  -Notes 'Generated via ai-service notebook pipeline' `
  -ReplaceExistingData
```

## Verification

The current repository state has been validated with:

- notebook pipeline execution succeeded
- backend import succeeded on the full dataset
- frontend production build succeeded
- backend test suite succeeded
- patient search, patient detail, and latest vitals endpoints returned real records

## App-specific documentation

- AI service: [apps/ai-service/README.md](apps/ai-service/README.md)
- Backend: [apps/backend/README.md](apps/backend/README.md)
- Frontend: [apps/frontend/README.md](apps/frontend/README.md)
