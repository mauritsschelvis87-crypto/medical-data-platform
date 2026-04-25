# AI Service

Python AI service plus the notebook pipeline used to turn the full raw dataset into normalized backend import files.

## What lives here

- FastAPI AI service
- notebook-based normalization flow
- full raw dataset
- generated processed and export datasets

## Active dataset source

The active raw dataset lives under:

- `datasets/raw_patients.csv`
- `datasets/observations.csv`
- `datasets/encounters.csv`
- `datasets/medications.csv`

These files are raw inputs. They are not in backend import format.

## Notebook pipeline

Notebook order:

1. `notebooks/01_load_raw.ipynb`
2. `notebooks/02_normalize_patients.ipynb`
3. `notebooks/03_normalize_vital_signs.ipynb`
4. `notebooks/04_build_features.ipynb`
5. `notebooks/05_export_for_import.ipynb`

Run the pipeline wrapper instead of executing notebooks manually.

## Run the pipeline

From `apps/ai-service`:

```powershell
venv\Scripts\python.exe scripts\run_notebook_pipeline.py --source full --export-name full
```

What this script does:

- copies the full raw files into `data/raw`
- mirrors them for notebook execution
- runs all notebooks in sequence
- synchronizes processed outputs into `data/processed` and `data/features`
- writes backend-ready CSV files into `data/exports/backend-import-full`
- keeps the 25-patient demo import under `data/exports/backend-import`

## Generated outputs

Notebook outputs:

- `data/processed/patient.csv`
- `data/processed/patient_address.csv`
- `data/processed/vital_signs.csv`
- `data/features/patient_features.csv`

Backend-ready import outputs:

- `data/exports/backend-import/patient.csv`
- `data/exports/backend-import/patient_address.csv`
- `data/exports/backend-import/vital_signs.csv`
- `data/exports/backend-import/patient_features.csv`
- `data/exports/backend-import/import_summary.csv`
- `data/exports/backend-import-full/patient.csv`
- `data/exports/backend-import-full/patient_address.csv`
- `data/exports/backend-import-full/vital_signs.csv`
- `data/exports/backend-import-full/patient_features.csv`
- `data/exports/backend-import-full/import_summary.csv`

## Why the backend export folder matters

The Spring Boot backend does not import the raw files directly.

It imports the generated normalized files from:

- `data/exports/backend-import`

The export runner already converts notebook output to the backend contract:

- camelCase column names where needed
- `patientId` based on source patient identifiers
- vital sign types aligned with backend enums
- `measuredAt` formatted for the Java importer
- Dutch-localized patient identity and addresses for frontend presentation

The `backend-import` folder is the compact demo scope used by the backend import script and contains 25 patients.

## Run the AI API

From `apps/ai-service`:

```powershell
venv\Scripts\activate
uvicorn app.main:app --reload --port 8001
```

AI service URL:

- `http://localhost:8001`

## Backend contract status

The AI request/response contract is aligned with the Spring backend for:

- UUID `patientId`
- flexible `features` map
- prediction types:
  - `CARDIOVASCULAR_RISK`
  - `DIABETES_RISK`
  - `GENERAL_DETERIORATION`
  - `SEPSIS_RISK`
  - `RESPIRATORY_RISK`
