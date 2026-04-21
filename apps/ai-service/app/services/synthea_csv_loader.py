from __future__ import annotations

from pathlib import Path

import pandas as pd


DEFAULT_SYNTHEA_CSV_DIR = Path(r"C:\Users\mauri\Projects\synthea\output\csv")

DEFAULT_TABLES = (
    "patients",
    "encounters",
    "observations",
    "medications",
)


def load_synthea_csv_tables(
    base_dir: Path | str = DEFAULT_SYNTHEA_CSV_DIR,
    tables: tuple[str, ...] = DEFAULT_TABLES,
) -> dict[str, pd.DataFrame]:
    csv_dir = Path(base_dir)
    if not csv_dir.exists():
        raise FileNotFoundError(f"Synthea CSV directory not found: {csv_dir}")

    loaded_tables: dict[str, pd.DataFrame] = {}
    for table in tables:
        csv_path = csv_dir / f"{table}.csv"
        if not csv_path.exists():
            raise FileNotFoundError(f"Expected CSV file not found: {csv_path}")
        loaded_tables[table] = pd.read_csv(csv_path)

    return loaded_tables
