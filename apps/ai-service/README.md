# AI Service

This service can load Synthea CSV exports from a local `datasets` folder.

## Dataset layout

The project expects CSV files under one of these locations:

- `SYNTHEA_CSV_DIR` environment variable, if set
- `datasets/` at the root of this app

For portfolio and demo use, a small sample dataset is included in `datasets/sample/`.

Expected files include:

- `patients.csv`
- `encounters.csv`
- `observations.csv`
- `medications.csv`

## Quick start

```powershell
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
$env:SYNTHEA_CSV_DIR = "datasets/sample"
uvicorn app.main:app --reload
```

## Full dataset

The full Synthea export is intentionally not tracked in Git. If you have the full CSV export, place the files in `datasets/` and either:

- leave `SYNTHEA_CSV_DIR` unset, or
- set `SYNTHEA_CSV_DIR` to the folder that contains the CSV files

## Notes

- `datasets/sample/` is meant for demo and testing only.
- `datasets/` is gitignored so large local datasets do not bloat the repository.
