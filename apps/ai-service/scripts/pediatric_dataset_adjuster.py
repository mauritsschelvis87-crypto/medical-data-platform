from __future__ import annotations

import argparse
import hashlib
from pathlib import Path

import pandas as pd


PEDIATRIC_TYPES = {
    "HEART_RATE",
    "BLOOD_PRESSURE_SYSTOLIC",
    "BLOOD_PRESSURE_DIASTOLIC",
    "BODY_TEMPERATURE",
    "OXYGEN_SATURATION",
    "WEIGHT",
    "HEIGHT",
    "BMI",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Adjust pediatric data to realistic age-aware values.")
    parser.add_argument(
        "--data-root",
        default=str(Path(__file__).resolve().parents[1] / "data"),
        help="Path to the ai-service data directory.",
    )
    return parser.parse_args()


def stable_index(seed: str, modulo: int) -> int:
    digest = hashlib.sha256(seed.encode("utf-8")).hexdigest()
    return int(digest[:16], 16) % modulo


def stable_fraction(seed: str) -> float:
    return stable_index(seed, 10_000) / 9_999


def rounded_value(seed: str, low: float, high: float, decimals: int = 1) -> float:
    value = low + (high - low) * stable_fraction(seed)
    return round(value, decimals)


def age_years(birth_date: pd.Timestamp, reference_date: pd.Timestamp) -> float:
    birth = pd.Timestamp(birth_date).tz_localize(None)
    reference = pd.Timestamp(reference_date).tz_localize(None)
    return max(0.0, (reference.normalize() - birth.normalize()).days / 365.25)


def age_months(birth_date: pd.Timestamp, reference_date: pd.Timestamp) -> float:
    birth = pd.Timestamp(birth_date).tz_localize(None)
    reference = pd.Timestamp(reference_date).tz_localize(None)
    return max(0.0, (reference.normalize() - birth.normalize()).days / 30.4375)


def pediatric_group(years: float) -> str | None:
    if years < 0:
        return None
    if years < 1:
        return "baby"
    if years < 3:
        return "toddler"
    if years < 12:
        return "child"
    if years < 18:
        return "adolescent"
    return None


def pediatric_profile(seed: str) -> str:
    score = stable_index(seed, 100)
    if score < 68:
        return "baseline"
    if score < 90:
        return "watch"
    return "high"


def expected_height_cm(years: float, gender: str, seed: str) -> float:
    if years < 1:
        months = min(age_months(pd.Timestamp("2000-01-01"), pd.Timestamp("2000-01-01") + pd.to_timedelta(years * 365.25, unit="D")), 12)
        base = 50.0 + min(months, 6) * 2.5 + max(0.0, months - 6) * 1.2
        return rounded_value(seed + "|height", base - 3.5, base + 3.5)
    if years < 3:
        base = 75.0 + (years - 1.0) * 9.0
        return rounded_value(seed + "|height", base - 4.0, base + 4.0)
    if years < 12:
        base = 95.0 + (years - 3.0) * 6.1
        return rounded_value(seed + "|height", base - 6.5, base + 6.5)
    if str(gender).upper() == "MALE":
        base = 149.0 + (years - 12.0) * 5.6
        return rounded_value(seed + "|height", base - 8.0, min(base + 8.0, 190.0))
    base = 150.0 + (years - 12.0) * 3.8
    return rounded_value(seed + "|height", base - 7.0, min(base + 7.0, 182.0))


def expected_weight_kg(years: float, gender: str, seed: str) -> float:
    if years < 1:
        months = min(age_months(pd.Timestamp("2000-01-01"), pd.Timestamp("2000-01-01") + pd.to_timedelta(years * 365.25, unit="D")), 12)
        base = 3.4 + min(months, 6) * 0.72 + max(0.0, months - 6) * 0.27
        return rounded_value(seed + "|weight", base - 0.8, base + 0.8)
    if years < 3:
        base = 9.5 + (years - 1.0) * 2.2
        return rounded_value(seed + "|weight", base - 1.5, base + 1.5)
    if years < 12:
        base = 14.0 + (years - 3.0) * 2.35
        return rounded_value(seed + "|weight", base - 3.0, base + 3.0)
    if str(gender).upper() == "MALE":
        base = 40.0 + (years - 12.0) * 5.2
        return rounded_value(seed + "|weight", base - 6.0, min(base + 6.0, 110.0))
    base = 41.0 + (years - 12.0) * 4.4
    return rounded_value(seed + "|weight", base - 6.0, min(base + 6.0, 105.0))


def realistic_weight_from_height(years: float, gender: str, height_cm: float, seed: str) -> float:
    profile = pediatric_profile(seed)
    if years < 2:
        return expected_weight_kg(years, gender, seed)

    if years < 12:
        bmi_ranges = {
            "baseline": (14.3, 17.1),
            "watch": (17.2, 20.4),
            "high": (20.5, 24.0),
        }
    else:
        bmi_ranges = {
            "baseline": (17.2, 22.2),
            "watch": (22.3, 26.8),
            "high": (26.9, 33.5),
        }
    bmi = rounded_value(seed + "|bmi-target", *bmi_ranges[profile])
    weight = bmi * (height_cm / 100.0) ** 2
    return round(weight, 1)


def latest_index_map(vital_signs_df: pd.DataFrame) -> dict[tuple[str, str], int]:
    sorted_df = vital_signs_df.sort_values("measured_at")
    latest_indices = (
        sorted_df.groupby(["source_patient_id", "vital_type"], sort=False)
        .tail(1)
        .index
    )
    return {
        (vital_signs_df.at[index, "source_patient_id"], vital_signs_df.at[index, "vital_type"]): index
        for index in latest_indices
    }


def assign_pediatric_vitals(patient_row: pd.Series, vital_signs_df: pd.DataFrame) -> tuple[pd.DataFrame, int]:
    updated_rows = 0
    patient_seed = str(patient_row["source_patient_id"])
    gender = str(patient_row.get("gender", "")).upper()
    birth_date = pd.to_datetime(patient_row["birth_date"], errors="coerce")
    if pd.isna(birth_date):
        return vital_signs_df, updated_rows

    latest_map = latest_index_map(vital_signs_df[vital_signs_df["source_patient_id"] == patient_seed])
    if not latest_map:
        return vital_signs_df, updated_rows

    latest_height_idx = latest_map.get((patient_seed, "HEIGHT"))
    latest_weight_idx = latest_map.get((patient_seed, "WEIGHT"))
    reference_index = latest_height_idx if latest_height_idx is not None else latest_weight_idx
    if reference_index is None:
        for vital_type in ("HEART_RATE", "BLOOD_PRESSURE_SYSTOLIC", "BLOOD_PRESSURE_DIASTOLIC"):
            reference_index = latest_map.get((patient_seed, vital_type))
            if reference_index is not None:
                break
    if reference_index is None:
        return vital_signs_df, updated_rows

    reference_date = pd.to_datetime(vital_signs_df.at[reference_index, "measured_at"], utc=True, errors="coerce")
    if pd.isna(reference_date):
        return vital_signs_df, updated_rows

    years = age_years(birth_date, reference_date)
    group = pediatric_group(years)
    if group is None:
        return vital_signs_df, updated_rows

    height_value = None
    weight_value = None

    if latest_height_idx is not None:
        current_height = float(vital_signs_df.at[latest_height_idx, "value"])
        target_height = expected_height_cm(years, gender, patient_seed)
        if current_height < 45 or current_height > 210:
            target_height = expected_height_cm(years, gender, patient_seed)
        else:
            target_height = round((current_height * 0.55) + (target_height * 0.45), 1)
        vital_signs_df.at[latest_height_idx, "value"] = target_height
        height_value = target_height
        updated_rows += 1

    if latest_weight_idx is not None:
        current_weight = float(vital_signs_df.at[latest_weight_idx, "value"])
        if height_value is None and latest_height_idx is not None:
            height_value = float(vital_signs_df.at[latest_height_idx, "value"])
        if height_value is not None:
            target_weight = realistic_weight_from_height(years, gender, height_value, patient_seed)
        else:
            target_weight = expected_weight_kg(years, gender, patient_seed)
        if current_weight < 1.5 or current_weight > 180:
            target_weight = target_weight
        else:
            target_weight = round((current_weight * 0.40) + (target_weight * 0.60), 1)
        vital_signs_df.at[latest_weight_idx, "value"] = target_weight
        weight_value = target_weight
        updated_rows += 1

    profile = pediatric_profile(patient_seed)

    hr_ranges = {
        "baby": {"baseline": (112, 148), "watch": (100, 160), "high": (145, 170)},
        "toddler": {"baseline": (98, 132), "watch": (90, 145), "high": (132, 150)},
        "child": {"baseline": (74, 98), "watch": (92, 112), "high": (110, 125)},
        "adolescent": {"baseline": (62, 88), "watch": (82, 98), "high": (96, 108)},
    }
    sys_ranges = {
        "baby": {"baseline": (74, 92), "watch": (90, 100), "high": (100, 110)},
        "toddler": {"baseline": (82, 100), "watch": (100, 108), "high": (108, 116)},
        "child": {"baseline": (92, 112), "watch": (112, 120), "high": (120, 130)},
        "adolescent": {"baseline": (102, 122), "watch": (122, 130), "high": (130, 142)},
    }
    dia_ranges = {
        "baby": {"baseline": (42, 58), "watch": (56, 64), "high": (64, 72)},
        "toddler": {"baseline": (46, 62), "watch": (60, 68), "high": (68, 76)},
        "child": {"baseline": (52, 70), "watch": (68, 76), "high": (76, 86)},
        "adolescent": {"baseline": (60, 76), "watch": (74, 82), "high": (82, 92)},
    }
    temp_ranges = {
        "baseline": (36.6, 37.3),
        "watch": (37.3, 37.9),
        "high": (37.9, 38.6),
    }
    spo2_ranges = {
        "baseline": (97.0, 100.0),
        "watch": (95.0, 97.0),
        "high": (92.0, 95.0),
    }

    for vital_type, ranges, decimals in [
        ("HEART_RATE", hr_ranges, 0),
        ("BLOOD_PRESSURE_SYSTOLIC", sys_ranges, 0),
        ("BLOOD_PRESSURE_DIASTOLIC", dia_ranges, 0),
    ]:
        idx = latest_map.get((patient_seed, vital_type))
        if idx is None:
            continue
        low, high = ranges[group][profile]
        vital_signs_df.at[idx, "value"] = rounded_value(f"{patient_seed}|{vital_type}", low, high, decimals)
        updated_rows += 1

    temp_idx = latest_map.get((patient_seed, "BODY_TEMPERATURE"))
    if temp_idx is not None:
        low, high = temp_ranges[profile]
        vital_signs_df.at[temp_idx, "value"] = rounded_value(f"{patient_seed}|BODY_TEMPERATURE", low, high, 1)
        updated_rows += 1

    spo2_idx = latest_map.get((patient_seed, "OXYGEN_SATURATION"))
    if spo2_idx is not None:
        low, high = spo2_ranges[profile]
        vital_signs_df.at[spo2_idx, "value"] = rounded_value(f"{patient_seed}|OXYGEN_SATURATION", low, high, 0)
        updated_rows += 1

    bmi_mask = (
        (vital_signs_df["source_patient_id"] == patient_seed)
        & (vital_signs_df["vital_type"] == "BMI")
    )
    if years < 2:
        if bmi_mask.any():
            vital_signs_df = vital_signs_df.loc[~bmi_mask].copy()
        return vital_signs_df, updated_rows

    if latest_height_idx is not None and latest_weight_idx is not None:
        if height_value is None:
            height_value = float(vital_signs_df.at[latest_height_idx, "value"])
        if weight_value is None:
            weight_value = float(vital_signs_df.at[latest_weight_idx, "value"])
        bmi_value = round(weight_value / ((height_value / 100.0) ** 2), 1)
        bmi_indices = vital_signs_df.loc[bmi_mask].sort_values("measured_at").index
        if len(bmi_indices) > 0:
            vital_signs_df.at[bmi_indices[-1], "value"] = bmi_value
            updated_rows += 1

    return vital_signs_df, updated_rows


def rebuild_patient_features(patient_df: pd.DataFrame, vital_signs_df: pd.DataFrame) -> pd.DataFrame:
    patient_columns = ["id", "patient_number", "birth_date", "gender"]
    features_df = patient_df[patient_columns].rename(columns={"id": "patient_id"}).copy()

    vital_signs_df = vital_signs_df.copy()
    vital_signs_df["measured_at"] = pd.to_datetime(vital_signs_df["measured_at"], utc=True, errors="coerce")
    latest_weight_times = (
        vital_signs_df.loc[vital_signs_df["vital_type"] == "WEIGHT", ["patient_id", "measured_at"]]
        .groupby("patient_id", as_index=False)["measured_at"]
        .max()
        .rename(columns={"measured_at": "latest_weight_time"})
    )

    latest_values = (
        vital_signs_df.sort_values("measured_at")
        .groupby(["patient_id", "vital_type"], as_index=False)
        .tail(1)
        .pivot(index="patient_id", columns="vital_type", values="value")
        .reset_index()
    )

    latest_30d = []
    for patient_id, patient_rows in vital_signs_df.groupby("patient_id"):
        row = {"patient_id": patient_id}
        for vital_type, vital_rows in patient_rows.groupby("vital_type"):
            max_time = vital_rows["measured_at"].max()
            window = vital_rows.loc[vital_rows["measured_at"] >= (max_time - pd.Timedelta(days=30))]
            row[f"avg_30d_{vital_type}"] = round(window["value"].astype(float).mean(), 3) if not window.empty else None
        latest_30d.append(row)
    avg_30d_df = pd.DataFrame(latest_30d)

    weight_change_rows = []
    for patient_id, patient_rows in vital_signs_df.loc[vital_signs_df["vital_type"] == "WEIGHT"].groupby("patient_id"):
        ordered = patient_rows.sort_values("measured_at")
        if len(ordered) >= 2:
            value = round(float(ordered.iloc[-1]["value"]) - float(ordered.iloc[0]["value"]), 3)
        else:
            value = 0.0
        weight_change_rows.append({"patient_id": patient_id, "weight_change": value})
    weight_change_df = pd.DataFrame(weight_change_rows)

    features_df = features_df.merge(latest_values, on="patient_id", how="left")
    features_df = features_df.merge(avg_30d_df, on="patient_id", how="left")
    features_df = features_df.merge(weight_change_df, on="patient_id", how="left")
    features_df = features_df.merge(latest_weight_times, on="patient_id", how="left")

    ordered_columns = [
        "patient_id",
        "patient_number",
        "birth_date",
        "gender",
        "BLOOD_PRESSURE_DIASTOLIC",
        "BLOOD_PRESSURE_SYSTOLIC",
        "BMI",
        "CHOLESTEROL",
        "GLUCOSE",
        "HEART_RATE",
        "HEIGHT",
        "OXYGEN_SATURATION",
        "TEMPERATURE",
        "WEIGHT",
        "avg_30d_BLOOD_PRESSURE_DIASTOLIC",
        "avg_30d_BLOOD_PRESSURE_SYSTOLIC",
        "avg_30d_BMI",
        "avg_30d_CHOLESTEROL",
        "avg_30d_GLUCOSE",
        "avg_30d_HEART_RATE",
        "avg_30d_HEIGHT",
        "avg_30d_OXYGEN_SATURATION",
        "avg_30d_TEMPERATURE",
        "avg_30d_WEIGHT",
        "weight_change",
        "latest_weight_time",
    ]
    for column in ordered_columns:
        if column not in features_df.columns:
            features_df[column] = pd.NA
    return features_df[ordered_columns]


def apply_pediatric_realism(data_root: Path) -> dict[str, int]:
    processed_dir = data_root / "processed"
    features_dir = data_root / "features"
    exports_dir = data_root / "exports"

    patient_path = processed_dir / "patient.csv"
    vital_signs_path = processed_dir / "vital_signs.csv"
    features_path = features_dir / "patient_features.csv"

    patient_df = pd.read_csv(patient_path)
    vital_signs_df = pd.read_csv(vital_signs_path)
    vital_signs_df["measured_at"] = pd.to_datetime(vital_signs_df["measured_at"], utc=True, errors="coerce")
    patient_df["birth_date"] = pd.to_datetime(patient_df["birth_date"], errors="coerce")

    updated_rows = 0
    pediatric_patients = 0

    for _, patient_row in patient_df.iterrows():
        source_patient_id = patient_row["source_patient_id"]
        patient_rows = vital_signs_df.loc[
            (vital_signs_df["source_patient_id"] == source_patient_id)
            & (vital_signs_df["vital_type"].isin(PEDIATRIC_TYPES))
        ]
        if patient_rows.empty:
            continue
        latest_date = patient_rows["measured_at"].max()
        years = age_years(patient_row["birth_date"], latest_date)
        if pediatric_group(years) is None:
            continue
        pediatric_patients += 1
        vital_signs_df, changed = assign_pediatric_vitals(patient_row, vital_signs_df)
        updated_rows += changed

    vital_signs_df = vital_signs_df.sort_values(["patient_id", "measured_at", "vital_type"]).reset_index(drop=True)
    features_df = rebuild_patient_features(patient_df, vital_signs_df)

    vital_signs_to_save = vital_signs_df.copy()
    vital_signs_to_save["measured_at"] = vital_signs_to_save["measured_at"].dt.strftime("%Y-%m-%d %H:%M:%S+00:00")
    vital_signs_to_save.to_csv(vital_signs_path, index=False)
    features_df.to_csv(features_path, index=False)

    export_vital_signs_path = exports_dir / "vital_signs.csv"
    export_features_path = exports_dir / "patient_features.csv"
    if export_vital_signs_path.exists():
        backend_view = vital_signs_df.rename(
            columns={
                "source_patient_id": "patientId",
                "vital_type": "type",
                "measured_at": "measuredAt",
                "observation_code": "sourceObservationCode",
                "description": "sourceDescription",
            }
        )[["patientId", "type", "value", "unit", "measuredAt", "sourceObservationCode", "sourceDescription"]]
        backend_view["type"] = backend_view["type"].replace({"TEMPERATURE": "BODY_TEMPERATURE"})
        backend_view["measuredAt"] = backend_view["measuredAt"].dt.strftime("%Y-%m-%dT%H:%M:%S")
        backend_view.to_csv(export_vital_signs_path, index=False)
    if export_features_path.exists():
        export_features_df = features_df.rename(
            columns={
                "patient_id": "patient_id",
                "patient_number": "patient_number",
                "birth_date": "birth_date",
            }
        )
        export_features_df.to_csv(export_features_path, index=False)

    return {
        "pediatricPatientsAdjusted": pediatric_patients,
        "vitalRowsUpdated": updated_rows,
    }


def main() -> int:
    args = parse_args()
    summary = apply_pediatric_realism(Path(args.data_root).resolve())
    print(summary)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
