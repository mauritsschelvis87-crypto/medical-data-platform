# Synthea Import

This project supports importing a public synthetic dataset from a standard Synthea CSV export.

## Why Synthea

Synthea is synthetic patient data, intended for safe software development and research workflows.

Official sources:

- https://synthetichealth.github.io/synthea/
- https://github.com/synthetichealth/synthea

## Required CSV files

Place these files in one directory:

- `patients.csv`
- `encounters.csv`
- `observations.csv`
- `medications.csv`

## Example import request

Send a POST request to:

`POST /api/datasets/import`

Example JSON:

```json
{
  "datasetName": "Synthea Full Import",
  "importType": "SYNTHEA_CSV",
  "normalizationVersion": "synthea-normalized-v1",
  "sourcePath": "C:/data/synthea/output/csv",
  "replaceExisting": true
}
```

## What gets normalized

The importer maps Synthea CSV data into:

- `patients`
- `vital_signs`
- `medication_catalog`
- `patient_medications`
- `consult_notes`
- `consult_note_versions`
- `timeline_events`

After import, prediction recalculation is triggered for imported patients.

## Current assumptions

- Medication dosage is normalized to `Standard dose` when the CSV does not provide dosage details.
- Medication frequency is normalized to `As prescribed`.
- Consult notes are generated from encounter descriptions.
- Weight staleness and other UX rules are handled in the frontend/backend after normalized storage.

## Notes

- This is intended for local development and portfolio use with public synthetic data.
- It is not intended as production clinical ETL.
