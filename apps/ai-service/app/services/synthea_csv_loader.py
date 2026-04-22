from __future__ import annotations

from pathlib import Path

import pandas as pd

from app.core.config import settings

PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SYNTHEA_CSV_DIR = PROJECT_ROOT / "datasets"

DEFAULT_TABLES = (
    "patients",
    "encounters",
    "observations",
    "medications",
)


def load_synthea_csv_tables(
    base_dir: Path | str | None = None,
    tables: tuple[str, ...] = DEFAULT_TABLES,
) -> dict[str, pd.DataFrame]:
    csv_dir = Path(base_dir or settings.synthea_csv_dir or DEFAULT_SYNTHEA_CSV_DIR)
    if not csv_dir.exists():
        raise FileNotFoundError(f"Synthea CSV directory not found: {csv_dir}")

    loaded_tables: dict[str, pd.DataFrame] = {}
    for table in tables:
        csv_path = csv_dir / f"{table}.csv"
        if not csv_path.exists():
            raise FileNotFoundError(f"Expected CSV file not found: {csv_path}")
        loaded_tables[table] = pd.read_csv(csv_path)

    return loaded_tables
