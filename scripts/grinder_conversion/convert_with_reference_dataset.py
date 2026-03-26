#!/usr/bin/env python3
"""
Convert grinder settings offline using the generated reference dataset.

This utility supports grinders present in:
  src/main/resources/grinder_conversion/data/free_grinders_reference_conversion.json
"""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any


@dataclass
class GrinderMath:
    grinder_id: str
    units: list[dict[str, Any]]
    bases: list[int]
    start: list[int]
    end: list[int]
    flat_start: int
    flat_end: int


def flatten_values(values: list[int], bases: list[int]) -> int:
    total = 0
    for i, value in enumerate(values):
        multiplier = 1
        for b in bases[i + 1 :]:
            multiplier *= b
        total += int(value) * multiplier
    return total


def unflatten_values(flat_value: int, bases: list[int]) -> list[int]:
    values = [0] * len(bases)
    rem = int(flat_value)
    for i in range(len(bases) - 1, -1, -1):
        base = bases[i]
        values[i] = rem % base
        rem //= base
    return values


def interpolate(x: float, points: list[tuple[float, float]]) -> float:
    if not points:
        raise ValueError("Interpolation requires at least one point.")

    points = sorted(points, key=lambda p: p[0])
    if x <= points[0][0]:
        return points[0][1]
    if x >= points[-1][0]:
        return points[-1][1]

    for i in range(1, len(points)):
        x1, y1 = points[i - 1]
        x2, y2 = points[i]
        if x1 <= x <= x2:
            if x2 == x1:
                return y1
            ratio = (x - x1) / (x2 - x1)
            return y1 + ratio * (y2 - y1)

    return points[-1][1]


def build_grinder_math(widget_grinder: dict[str, Any]) -> GrinderMath:
    units = widget_grinder["units"]
    bases = [int(u["maximum"]) + 1 for u in units]
    start = [int(v) for v in widget_grinder["range"]["grinder"]["start"]]
    end = [int(v) for v in widget_grinder["range"]["grinder"]["end"]]
    return GrinderMath(
        grinder_id=widget_grinder["id"],
        units=units,
        bases=bases,
        start=start,
        end=end,
        flat_start=flatten_values(start, bases),
        flat_end=flatten_values(end, bases),
    )


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def parse_setting_input(setting_text: str) -> dict[str, int]:
    out: dict[str, int] = {}
    if not setting_text:
        return out
    parts = [p.strip() for p in setting_text.split(",") if p.strip()]
    for part in parts:
        if "=" not in part:
            raise ValueError(f"Invalid setting token '{part}'. Expected format: label=value")
        key, value = [s.strip() for s in part.split("=", 1)]
        out[key] = int(value)
    return out


def format_setting(units: list[dict[str, Any]], values: list[int]) -> str:
    pairs = [f"{u['label']}={int(values[idx])}" for idx, u in enumerate(units)]
    return ", ".join(pairs)


def main() -> None:
    parser = argparse.ArgumentParser(description="Offline grinder conversion using reference mappings.")
    parser.add_argument(
        "--dataset",
        default="src/main/resources/grinder_conversion/data/free_grinders_reference_conversion.json",
        help="Path to free_grinders_reference_conversion.json",
    )
    parser.add_argument(
        "--grinders",
        default="src/main/resources/grinder_conversion/data/beean_widget_grinders.json",
        help="Path to beean_widget_grinders.json",
    )
    parser.add_argument("--source", required=True, help="Source grinder id")
    parser.add_argument("--target", required=True, help="Target grinder id")
    parser.add_argument(
        "--setting",
        required=True,
        help="Comma-separated setting pairs. Example: click=20 OR number=4,click=2",
    )
    args = parser.parse_args()

    dataset = load_json(Path(args.dataset))
    grinders = load_json(Path(args.grinders))
    grinder_by_id = {g["id"]: g for g in grinders}

    source_grinder = grinder_by_id.get(args.source)
    target_grinder = grinder_by_id.get(args.target)
    if not source_grinder:
        raise ValueError(f"Unknown source grinder: {args.source}")
    if not target_grinder:
        raise ValueError(f"Unknown target grinder: {args.target}")

    source_math = build_grinder_math(source_grinder)
    target_math = build_grinder_math(target_grinder)

    setting_input = parse_setting_input(args.setting)
    source_values = []
    for unit in source_math.units:
        label = unit["label"]
        source_values.append(int(setting_input.get(label, 0)))
    source_flat_raw = flatten_values(source_values, source_math.bases)
    source_flat = min(max(source_flat_raw, source_math.flat_start), source_math.flat_end)
    source_values = unflatten_values(source_flat, source_math.bases)

    reference_id = dataset["reference_grinder_id"]
    entry_by_grinder = {g["grinder_id"]: g for g in dataset["grinders"]}
    source_entry = entry_by_grinder.get(args.source)
    target_entry = entry_by_grinder.get(args.target)
    if source_entry is None or target_entry is None:
        raise ValueError(
            "Source/target grinder not available in reference dataset "
            "(currently built from free grinders only)."
        )

    if source_entry["to_reference"] is None or target_entry["from_reference"] is None:
        raise ValueError("Source/target mapping is not available due preview-limited data.")

    to_ref_points = [
        (float(item["source_flat"]), float(item["target_flat_raw"]))
        for item in source_entry["to_reference"]["mappings"]
    ]
    ref_flat_est = interpolate(float(source_flat), to_ref_points)

    from_ref_points = [
        (float(item["source_flat"]), float(item["target_flat_clamped"]))
        for item in target_entry["from_reference"]["mappings"]
    ]
    target_flat_est = interpolate(ref_flat_est, from_ref_points)
    target_flat_rounded = int(round(target_flat_est))
    target_flat = min(max(target_flat_rounded, target_math.flat_start), target_math.flat_end)
    target_values = unflatten_values(target_flat, target_math.bases)

    print("reference_grinder_id:", reference_id)
    print("source:", args.source, "|", format_setting(source_math.units, source_values))
    print("target:", args.target, "|", format_setting(target_math.units, target_values))
    print("source_flat:", source_flat)
    print("reference_flat_estimated:", round(ref_flat_est, 3))
    print("target_flat_estimated:", round(target_flat_est, 3))


if __name__ == "__main__":
    main()
