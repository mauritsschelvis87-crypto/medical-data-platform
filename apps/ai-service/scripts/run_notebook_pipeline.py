from __future__ import annotations

import argparse
import json
import hashlib
import os
import re
import shutil
import subprocess
import time
import urllib.error
import urllib.request
from pathlib import Path

import pandas as pd

from pediatric_dataset_adjuster import apply_pediatric_realism


ROOT_DIR = Path(__file__).resolve().parents[1]
NOTEBOOK_DIR = ROOT_DIR / "notebooks"
RAW_SOURCE_DIR = ROOT_DIR / "datasets"
DATA_DIR = ROOT_DIR / "data"
RAW_DIR = DATA_DIR / "raw"
PROCESSED_DIR = DATA_DIR / "processed"
FEATURES_DIR = DATA_DIR / "features"
EXPORT_DIR = DATA_DIR / "exports"
NOTEBOOK_DATA_DIR = NOTEBOOK_DIR / "data"
NOTEBOOK_RAW_DIR = NOTEBOOK_DATA_DIR / "raw"
NOTEBOOK_PROCESSED_DIR = NOTEBOOK_DATA_DIR / "processed"
NOTEBOOK_FEATURES_DIR = NOTEBOOK_DATA_DIR / "features"
NOTEBOOK_EXPORT_DIR = NOTEBOOK_DATA_DIR / "exports"

NOTEBOOKS = [
    "01_load_raw.ipynb",
    "02_normalize_patients.ipynb",
    "03_normalize_vital_signs.ipynb",
    "04_build_features.ipynb",
    "05_export_for_import.ipynb",
]

RAW_FILE_MAPPING = {
    "raw_patients.csv": "patients.csv",
    "observations.csv": "observations.csv",
    "encounters.csv": "encounters.csv",
    "medications.csv": "medications.csv",
}

MALE_FIRST_NAMES = [
    "Jan",
    "Lars",
    "Daan",
    "Milan",
    "Thomas",
    "Sem",
    "Noah",
    "Luuk",
    "Bram",
    "Finn",
    "Tijn",
    "Hugo",
]

FEMALE_FIRST_NAMES = [
    "Sophie",
    "Emma",
    "Julia",
    "Eva",
    "Anna",
    "Lotte",
    "Mila",
    "Nina",
    "Sara",
    "Fenna",
    "Isabelle",
    "Roos",
]

NEUTRAL_FIRST_NAMES = [
    "Sam",
    "Robin",
    "Alex",
    "Jules",
    "Noa",
    "Mika",
]

LAST_NAMES = [
    "de Vries",
    "van Dijk",
    "Meijer",
    "Bakker",
    "Visser",
    "Smit",
    "de Boer",
    "Mulder",
    "de Jong",
    "Vos",
    "Peters",
    "Hendriks",
    "van den Berg",
    "Jacobs",
    "Willems",
    "Bos",
    "de Groot",
    "van Leeuwen",
    "Schouten",
    "Prins",
]

DUTCH_ADDRESS_CATALOG = [
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Haarlemmerstraat"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Breestraat"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Nieuwe Rijn"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Steenschuur"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Hooigracht"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Langegracht"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Korevaarstraat"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Doezastraat"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Herengracht"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Morsstraat"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Witte Singel"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Levendaal"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Pieterskerk-Choorsteeg"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Boerhaavelaan"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Rapenburg"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Kaiserstraat"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Janvossensteeg"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Maresingel"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Rijndijkstraat"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Papengracht"},
    {"city": "Leiden", "state": "Zuid-Holland", "county": "Leiden", "street": "Gerecht"},
    {"city": "Voorschoten", "state": "Zuid-Holland", "county": "Voorschoten", "street": "Schoolstraat"},
    {"city": "Voorschoten", "state": "Zuid-Holland", "county": "Voorschoten", "street": "Voorstraat"},
    {"city": "Zoeterwoude", "state": "Zuid-Holland", "county": "Zoeterwoude", "street": "Dorpstraat"},
    {"city": "Zoeterwoude", "state": "Zuid-Holland", "county": "Zoeterwoude", "street": "Hoge Rijndijk"},
    {"city": "Leiderdorp", "state": "Zuid-Holland", "county": "Leiderdorp", "street": "Hoofdstraat"},
    {"city": "Leiderdorp", "state": "Zuid-Holland", "county": "Leiderdorp", "street": "Oranjegalerij"},
    {"city": "Oegstgeest", "state": "Zuid-Holland", "county": "Oegstgeest", "street": "Rhijngeesterstraatweg"},
    {"city": "Zoetermeer", "state": "Zuid-Holland", "county": "Zoetermeer", "street": "Dorpsstraat"},
    {"city": "Leidschendam", "state": "Zuid-Holland", "county": "Leidschendam-Voorburg", "street": "Damlaan"},
    {"city": "Den Haag", "state": "Zuid-Holland", "county": "Den Haag", "street": "Spui"},
    {"city": "Valkenburg (ZH)", "state": "Zuid-Holland", "county": "Katwijk", "street": "Hoofdstraat"},
    {"city": "Breda", "state": "Noord-Brabant", "county": "Breda", "street": "Ginnekenweg"},
    {"city": "Utrecht", "state": "Utrecht", "county": "Utrecht", "street": "Oudegracht"},
    {"city": "Den Helder", "state": "Noord-Holland", "county": "Den Helder", "street": "Beatrixstraat"},
]

DUTCH_POSTAL_CODES = {
    "Leiden": ["2312AB", "2313CD", "2314GH", "2321JK", "2331LM", "2332NP"],
    "Voorschoten": ["2251AA", "2252BC", "2253DE"],
    "Zoeterwoude": ["2381AB", "2382CD"],
    "Leiderdorp": ["2351AA", "2352BC", "2353DE"],
    "Oegstgeest": ["2341AB", "2342CD"],
    "Zoetermeer": ["2711AA", "2722BC"],
    "Leidschendam": ["2261AA", "2262BC"],
    "Den Haag": ["2511AA", "2562BC"],
    "Valkenburg (ZH)": ["2235AB", "2235CD"],
    "Breda": ["4811AA", "4812BC"],
    "Utrecht": ["3511AA", "3521BC"],
    "Den Helder": ["1781AA", "1782BC"],
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run the notebook pipeline and prepare backend import files.")
    parser.add_argument(
        "--export-name",
        choices=["full"],
        default="full",
        help="Name of the backend export folder under data/exports.",
    )
    parser.add_argument(
        "--import-backend",
        action="store_true",
        help="Post the generated backend-import files to the running Spring backend.",
    )
    parser.add_argument(
        "--backend-url",
        default="http://localhost:8081",
        help="Spring backend base URL used with --import-backend.",
    )
    parser.add_argument(
        "--replace-existing-data",
        action="store_true",
        help="Request the backend importer to refresh existing imported records for matching patients.",
    )
    return parser.parse_args()


def resolve_export_directory(export_name: str) -> Path:
    return EXPORT_DIR / f"backend-import-{export_name}"


def ensure_directories(backend_export_dir: Path) -> None:
    for directory in [
        RAW_DIR,
        PROCESSED_DIR,
        FEATURES_DIR,
        EXPORT_DIR,
        backend_export_dir,
        NOTEBOOK_RAW_DIR,
        NOTEBOOK_PROCESSED_DIR,
        NOTEBOOK_FEATURES_DIR,
        NOTEBOOK_EXPORT_DIR,
    ]:
        directory.mkdir(parents=True, exist_ok=True)


def stage_raw_files(source_dir: Path) -> None:
    staged_files = {source_dir / source_name: target_name for source_name, target_name in RAW_FILE_MAPPING.items()}

    missing_sources = [str(source) for source in staged_files if not source.exists()]
    if missing_sources:
        raise FileNotFoundError(f"Missing source files: {missing_sources}")

    for source, target_name in staged_files.items():
        raw_target = RAW_DIR / target_name
        notebook_target = NOTEBOOK_RAW_DIR / target_name
        shutil.copy2(source, raw_target)
        shutil.copy2(source, notebook_target)
        print(f"Staged {source.relative_to(ROOT_DIR)} -> {raw_target.relative_to(ROOT_DIR)}")


def execute_notebooks() -> None:
    nbconvert = ROOT_DIR / "venv" / "Scripts" / "jupyter-nbconvert.exe"
    if not nbconvert.exists():
        raise FileNotFoundError(f"jupyter-nbconvert not found at {nbconvert}")

    for notebook_name in NOTEBOOKS:
        notebook_path = NOTEBOOK_DIR / notebook_name
        started_at = time.perf_counter()
        print(f"Executing {notebook_path.relative_to(ROOT_DIR)}")
        env = os.environ.copy()
        env["PYTHONWARNINGS"] = (
            "ignore:Proactor event loop does not implement add_reader family of methods required for zmq:RuntimeWarning"
        )
        completed = subprocess.run(
            [
                str(nbconvert),
                "--to",
                "notebook",
                "--execute",
                "--inplace",
                str(notebook_path),
            ],
            cwd=ROOT_DIR,
            env=env,
            capture_output=True,
            text=True,
        )
        if completed.returncode != 0:
            raise subprocess.CalledProcessError(
                completed.returncode,
                completed.args,
                output=completed.stdout,
                stderr=completed.stderr,
            )
        output_lines = [
            line
            for line in (completed.stdout.splitlines() + completed.stderr.splitlines())
            if line
            and "Proactor event loop does not implement add_reader family of methods required for zmq" not in line
            and "self._get_loop()" not in line
            and "Assertion failed: Connection reset by peer [10054]" not in line
            and "signaler.cpp:345" not in line
        ]
        for line in output_lines:
            print(line)
        print(
            f"Completed {notebook_path.name} in {time.perf_counter() - started_at:.1f}s"
        )


def sync_notebook_outputs() -> None:
    for source_dir, target_dir, file_names in [
        (NOTEBOOK_PROCESSED_DIR, PROCESSED_DIR, ["patient.csv", "patient_address.csv", "vital_signs.csv"]),
        (NOTEBOOK_FEATURES_DIR, FEATURES_DIR, ["patient_features.csv"]),
        (
            NOTEBOOK_EXPORT_DIR,
            EXPORT_DIR,
            ["patient.csv", "patient_address.csv", "vital_signs.csv", "patient_features.csv"],
        ),
    ]:
        for file_name in file_names:
            source = source_dir / file_name
            if source.exists():
                shutil.copy2(source, target_dir / file_name)


def clean_import_names(patient_df: pd.DataFrame) -> pd.DataFrame:
    def clean_name(value: object) -> object:
        if pd.isna(value):
            return value
        cleaned = re.sub(r"\d+$", "", str(value)).strip()
        return cleaned or value

    patient_df = patient_df.copy()
    patient_df["first_name"] = patient_df["first_name"].apply(clean_name)
    patient_df["last_name"] = patient_df["last_name"].apply(clean_name)
    patient_df["full_name"] = (
        patient_df["first_name"].fillna("").astype(str).str.strip()
        + " "
        + patient_df["last_name"].fillna("").astype(str).str.strip()
    ).str.strip()
    patient_df["full_name"] = patient_df["full_name"].replace("", pd.NA)
    return patient_df


def stable_index(seed: str, modulo: int) -> int:
    digest = hashlib.sha256(seed.encode("utf-8")).hexdigest()
    return int(digest[:16], 16) % modulo


def localize_patient_identity(patient_export_df: pd.DataFrame) -> pd.DataFrame:
    localized_df = patient_export_df.copy()
    first_names: list[str] = []
    last_names: list[str] = []
    full_names: list[str] = []

    for row in localized_df.itertuples(index=False):
        seed = f"{row.patientNumber}|{row.sourcePatientId}"
        gender = str(row.gender).upper() if pd.notna(row.gender) else ""
        if gender == "MALE":
            first_name_pool = MALE_FIRST_NAMES
        elif gender == "FEMALE":
            first_name_pool = FEMALE_FIRST_NAMES
        else:
            first_name_pool = NEUTRAL_FIRST_NAMES

        first_name = first_name_pool[stable_index(seed + "|first", len(first_name_pool))]
        last_name = LAST_NAMES[stable_index(seed + "|last", len(LAST_NAMES))]
        full_name = f"{first_name} {last_name}"

        first_names.append(first_name)
        last_names.append(last_name)
        full_names.append(full_name)

    localized_df["firstName"] = first_names
    localized_df["lastName"] = last_names
    localized_df["fullName"] = full_names
    return localized_df


def localize_patient_addresses(patient_address_export_df: pd.DataFrame) -> pd.DataFrame:
    localized_df = patient_address_export_df.copy()
    address_lines: list[str] = []
    cities: list[str] = []
    states: list[str] = []
    counties: list[str] = []
    postal_codes: list[str] = []

    for row in localized_df.itertuples(index=False):
        seed = str(row.patientId)
        template = DUTCH_ADDRESS_CATALOG[stable_index(seed + "|addr", len(DUTCH_ADDRESS_CATALOG))]
        house_number = stable_index(seed + "|house", 220) + 1
        suffix_index = stable_index(seed + "|suffix", 8)
        suffix = "" if suffix_index < 5 else chr(ord("A") + suffix_index - 5)
        postal_pool = DUTCH_POSTAL_CODES[template["city"]]
        postal_code = postal_pool[stable_index(seed + "|zip", len(postal_pool))]
        address_line = f"{template['street']} {house_number}{suffix}"

        address_lines.append(address_line)
        cities.append(template["city"])
        states.append(template["state"])
        counties.append(template["county"])
        postal_codes.append(postal_code)

    localized_df["addressLine"] = address_lines
    localized_df["city"] = cities
    localized_df["state"] = states
    localized_df["county"] = counties
    localized_df["zipCode"] = postal_codes
    return localized_df


def export_backend_import_files(source: str, backend_export_dir: Path) -> None:
    patient_df = pd.read_csv(PROCESSED_DIR / "patient.csv")
    patient_address_df = pd.read_csv(PROCESSED_DIR / "patient_address.csv")
    vital_signs_df = pd.read_csv(PROCESSED_DIR / "vital_signs.csv")
    patient_features_df = pd.read_csv(FEATURES_DIR / "patient_features.csv")

    patient_df = clean_import_names(patient_df)

    patient_export_df = patient_df.rename(
        columns={
            "patient_number": "patientNumber",
            "source_patient_id": "sourcePatientId",
            "first_name": "firstName",
            "last_name": "lastName",
            "full_name": "fullName",
            "birth_date": "birthDate",
            "death_date": "deathDate",
            "marital_status": "maritalStatus",
        }
    )[
        [
            "patientNumber",
            "sourcePatientId",
            "firstName",
            "lastName",
            "fullName",
            "birthDate",
            "gender",
            "deceased",
            "deathDate",
            "maritalStatus",
            "race",
            "ethnicity",
        ]
    ]
    patient_export_df = localize_patient_identity(patient_export_df)

    patient_lookup_df = patient_df[["id", "source_patient_id"]].rename(
        columns={
            "id": "patient_id",
            "source_patient_id": "sourcePatientId",
        }
    )

    patient_address_export_df = patient_address_df.merge(patient_lookup_df, on="patient_id", how="inner")
    patient_address_export_df = patient_address_export_df.rename(
        columns={
            "sourcePatientId": "patientId",
            "address_line": "addressLine",
            "zip_code": "zipCode",
        }
    )[
        [
            "patientId",
            "addressLine",
            "city",
            "state",
            "county",
            "zipCode",
        ]
    ]
    patient_address_export_df = localize_patient_addresses(patient_address_export_df)

    vital_signs_export_df = vital_signs_df.rename(
        columns={
            "source_patient_id": "patientId",
            "vital_type": "type",
            "measured_at": "measuredAt",
            "observation_code": "sourceObservationCode",
            "description": "sourceDescription",
        }
    )[
        [
            "patientId",
            "type",
            "value",
            "unit",
            "measuredAt",
            "sourceObservationCode",
            "sourceDescription",
        ]
    ]
    vital_signs_export_df["type"] = vital_signs_export_df["type"].replace({"TEMPERATURE": "BODY_TEMPERATURE"})
    vital_signs_export_df["measuredAt"] = pd.to_datetime(
        vital_signs_export_df["measuredAt"], errors="coerce", utc=True
    ).dt.strftime("%Y-%m-%dT%H:%M:%S")

    import_summary_df = pd.DataFrame(
        [
            {
                "recordsReceived": len(patient_export_df) + len(patient_address_export_df) + len(vital_signs_export_df),
                "recordsProcessed": len(patient_export_df) + len(patient_address_export_df) + len(vital_signs_export_df),
                "recordsFailed": 0,
                "patientCount": len(patient_export_df),
                "patientAddressCount": len(patient_address_export_df),
                "vitalSignsCount": len(vital_signs_export_df),
                "skippedRecords": 0,
                "validationSummary": f"Generated from ai-service notebook pipeline ({source})",
            }
        ]
    )

    patient_export_df.to_csv(backend_export_dir / "patient.csv", index=False)
    patient_address_export_df.to_csv(backend_export_dir / "patient_address.csv", index=False)
    vital_signs_export_df.to_csv(backend_export_dir / "vital_signs.csv", index=False)
    patient_features_df.to_csv(backend_export_dir / "patient_features.csv", index=False)
    import_summary_df.to_csv(backend_export_dir / "import_summary.csv", index=False)

    print(f"Wrote backend import files to {backend_export_dir.relative_to(ROOT_DIR)}")


def import_backend_dataset(
    backend_url: str,
    backend_export_dir: Path,
    source: str,
    replace_existing_data: bool,
) -> None:
    request_body = {
        "sourceName": f"ai-service-{source}",
        "datasetType": "NORMALIZED_MEDICAL_DATA",
        "sourceDirectoryPath": str(backend_export_dir.resolve()),
        "notes": "Generated via ai-service notebook pipeline",
        "replaceExistingData": replace_existing_data,
    }
    payload = json.dumps(request_body).encode("utf-8")
    request = urllib.request.Request(
        url=f"{backend_url.rstrip('/')}/api/dataset-imports",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    print(f"Importing generated dataset into backend at {backend_url}")
    try:
        with urllib.request.urlopen(request, timeout=300) as response:
            response_body = response.read().decode("utf-8")
    except urllib.error.URLError as exception:
        raise RuntimeError(f"Backend import request failed: {exception}") from exception

    print("Backend import completed.")
    print(response_body)


def main() -> int:
    args = parse_args()
    source = "full"
    source_dir = RAW_SOURCE_DIR
    backend_export_dir = resolve_export_directory(args.export_name)

    ensure_directories(backend_export_dir)
    stage_raw_files(source_dir)
    execute_notebooks()
    sync_notebook_outputs()
    summary = apply_pediatric_realism(DATA_DIR)
    print(f"Applied pediatric dataset realism: {summary}")
    export_backend_import_files(source, backend_export_dir)
    if args.import_backend:
        import_backend_dataset(args.backend_url, backend_export_dir, source, args.replace_existing_data)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
