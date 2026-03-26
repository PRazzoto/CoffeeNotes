#!/usr/bin/env python3
"""
Build grinder conversion research artifacts from public web sources.

Outputs:
  - src/main/resources/grinder_conversion/data/beean_widget_grinders.json
  - src/main/resources/grinder_conversion/data/beean_grind_settings_pages.json
  - src/main/resources/grinder_conversion/data/free_grinders_reference_conversion.json
  - src/main/resources/grinder_conversion/data/grinder_conversion_dataset.json
"""

from __future__ import annotations

import argparse
import json
import math
import re
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import requests
from bs4 import BeautifulSoup


BEEAN_SITE = "https://beeancoffee.com"
WIDGET_BASE = "https://converter.beeancoffee.com"
GRINDERS_ENDPOINT = f"{WIDGET_BASE}/api/widget/v1/grinders"
CONVERT_ENDPOINT = f"{WIDGET_BASE}/api/widget/v1/convert"
PREVIEW_ENDPOINT = f"{WIDGET_BASE}/api/widget/v1/preview-conversion"
PAGE_SITEMAP = f"{BEEAN_SITE}/page-sitemap.xml"
REFERENCE_GRINDER_ID = "comandante_c40"


REQUEST_HEADERS = {
    "Origin": BEEAN_SITE,
    "Referer": f"{BEEAN_SITE}/",
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/122.0.0.0 Safari/537.36"
    ),
}

JSON_HEADERS = {
    **REQUEST_HEADERS,
    "Content-Type": "application/json",
}


METHOD_RANGE_RE = re.compile(
    r"(?:between|from)\s*(\d+(?:\.\d+)?)\s*[-–—to]+\s*(\d+(?:\.\d+)?)\s*microns?",
    flags=re.IGNORECASE,
)

METHOD_NAME_RE = re.compile(
    r"range\s+for\s+(.+?)\s+brewing\s+method",
    flags=re.IGNORECASE,
)

MICRONS_PER_RE = re.compile(
    r"(\d+(?:\.\d+)?)\s*microns?\s*per\s*([a-zA-Z/-]+)",
    flags=re.IGNORECASE,
)

EACH_STEP_RE = re.compile(
    r"each\s+([a-zA-Z/-]+)\s+(\d+(?:\.\d+)?)\s*microns?",
    flags=re.IGNORECASE,
)

WHOLE_NUMBER_RE = re.compile(r"^\d+(?:\.\d+)?$")


def ensure_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def now_utc_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def to_ascii_safe(value: str) -> str:
    return " ".join(value.replace("\u00a0", " ").split())


def get_json(session: requests.Session, url: str, headers: dict[str, str] | None = None) -> Any:
    response = session.get(url, headers=headers, timeout=30)
    response.raise_for_status()
    return response.json()


def post_json(
    session: requests.Session,
    url: str,
    payload: dict[str, Any],
    headers: dict[str, str] | None = None,
) -> Any:
    response = session.post(
        url,
        headers=headers,
        data=json.dumps(payload),
        timeout=30,
    )
    response.raise_for_status()
    return response.json()


def extract_path_slug(url: str) -> str:
    trimmed = url.rstrip("/")
    return trimmed.rsplit("/", 1)[-1]


def parse_method_name(question: str, answer: str) -> str | None:
    answer_match = METHOD_NAME_RE.search(answer)
    if answer_match:
        return to_ascii_safe(answer_match.group(1))

    # Fallback: derive from question ending after "for".
    q = question.strip().rstrip("?")
    lower_q = q.lower()
    marker = " for "
    if marker in lower_q:
        idx = lower_q.rfind(marker)
        return to_ascii_safe(q[idx + len(marker) :])
    return None


def parse_method_range(answer: str) -> dict[str, float] | None:
    match = METHOD_RANGE_RE.search(answer)
    if not match:
        return None
    low = float(match.group(1))
    high = float(match.group(2))
    return {
        "min_microns": min(low, high),
        "max_microns": max(low, high),
    }


def parse_microns_per_candidates(text: str) -> list[dict[str, Any]]:
    candidates: list[dict[str, Any]] = []

    for m in MICRONS_PER_RE.finditer(text):
        value = float(m.group(1))
        unit = m.group(2).lower().strip()
        candidates.append(
            {
                "value_microns": value,
                "per_unit": unit,
                "extract_pattern": "microns_per",
            }
        )

    for m in EACH_STEP_RE.finditer(text):
        unit = m.group(1).lower().strip()
        value = float(m.group(2))
        candidates.append(
            {
                "value_microns": value,
                "per_unit": unit,
                "extract_pattern": "each_unit",
            }
        )

    deduped: list[dict[str, Any]] = []
    seen: set[tuple[float, str, str]] = set()
    for c in candidates:
        key = (c["value_microns"], c["per_unit"], c["extract_pattern"])
        if key in seen:
            continue
        seen.add(key)
        deduped.append(c)
    return deduped


def scrape_grind_settings_pages(session: requests.Session) -> list[dict[str, Any]]:
    sitemap_xml = session.get(PAGE_SITEMAP, headers=REQUEST_HEADERS, timeout=30).text
    soup = BeautifulSoup(sitemap_xml, "xml")
    urls = [loc.get_text(strip=True) for loc in soup.find_all("loc")]
    target_urls = sorted(
        {
            url
            for url in urls
            if url.startswith(BEEAN_SITE)
            and url.endswith("/")
            and "-grind-settings/" in url
            and "wp-content" not in url
        }
    )

    pages: list[dict[str, Any]] = []
    for idx, url in enumerate(target_urls, start=1):
        html = session.get(url, headers=REQUEST_HEADERS, timeout=30).text
        page_soup = BeautifulSoup(html, "html.parser")
        title = to_ascii_safe(page_soup.title.get_text(" ", strip=True)) if page_soup.title else ""
        h1 = page_soup.find("h1")
        h1_text = to_ascii_safe(h1.get_text(" ", strip=True)) if h1 else ""
        slug = extract_path_slug(url)

        qa_items: list[dict[str, Any]] = []
        method_ranges: list[dict[str, Any]] = []
        micron_candidates: list[dict[str, Any]] = []

        for h3 in page_soup.find_all("h3"):
            question = to_ascii_safe(h3.get_text(" ", strip=True))
            answer_node = h3.find_next_sibling("p")
            answer = to_ascii_safe(answer_node.get_text(" ", strip=True)) if answer_node else ""

            if question or answer:
                qa_items.append({"question": question, "answer": answer})

            method_name = parse_method_name(question, answer)
            range_data = parse_method_range(answer)
            if method_name and range_data:
                method_ranges.append(
                    {
                        "method": method_name,
                        **range_data,
                        "source_question": question,
                        "source_answer": answer,
                    }
                )

            if answer:
                candidates = parse_microns_per_candidates(answer)
                for c in candidates:
                    micron_candidates.append(
                        {
                            **c,
                            "source_question": question,
                            "source_answer": answer,
                        }
                    )

        pages.append(
            {
                "url": url,
                "slug": slug,
                "title": title,
                "h1": h1_text,
                "qa_count": len(qa_items),
                "method_ranges": method_ranges,
                "microns_per_candidates": micron_candidates,
                "qa_items": qa_items,
                "extracted_at_utc": now_utc_iso(),
                "source_index": idx,
                "source_total": len(target_urls),
            }
        )
        # Keep a small delay to reduce server burst.
        time.sleep(0.08)

    return pages


@dataclass
class GrinderMath:
    id: str
    units: list[dict[str, Any]]
    bases: list[int]
    start: list[int]
    end: list[int]
    flat_start: int
    flat_end: int
    total_positions: int


def build_grinder_math(grinder: dict[str, Any]) -> GrinderMath:
    units = grinder["units"]
    bases = [int(u["maximum"]) + 1 for u in units]
    start = [int(v) for v in grinder["range"]["grinder"]["start"]]
    end = [int(v) for v in grinder["range"]["grinder"]["end"]]
    flat_start = flatten_values(start, bases)
    flat_end = flatten_values(end, bases)
    return GrinderMath(
        id=grinder["id"],
        units=units,
        bases=bases,
        start=start,
        end=end,
        flat_start=flat_start,
        flat_end=flat_end,
        total_positions=(flat_end - flat_start + 1),
    )


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
        b = bases[i]
        values[i] = rem % b
        rem //= b
    return values


def dict_from_units(units: list[dict[str, Any]], values: list[int]) -> dict[str, int]:
    return {u["label"]: int(values[idx]) for idx, u in enumerate(units)}


def values_from_response_units(units: list[dict[str, Any]], response: dict[str, Any]) -> list[int]:
    values: list[int] = []
    for unit in units:
        label = unit["label"]
        raw = response.get(label, 0)
        if isinstance(raw, str):
            if WHOLE_NUMBER_RE.match(raw.strip()):
                raw = float(raw.strip())
            else:
                raw = 0
        values.append(int(round(float(raw))))
    return values


def evenly_spaced_ints(start: int, end: int, count: int) -> list[int]:
    if count <= 1:
        return [start]
    if start == end:
        return [start]
    points = []
    for i in range(count):
        ratio = i / (count - 1)
        value = int(round(start + ratio * (end - start)))
        points.append(value)
    return sorted(set(points))


def preview_mode(
    session: requests.Session,
    grinder1_id: str,
    grinder2_id: str,
) -> dict[str, Any]:
    payload = {"grinder1": grinder1_id, "grinder2": grinder2_id}
    data = post_json(session, PREVIEW_ENDPOINT, payload, headers=JSON_HEADERS)
    return data


def convert_settings(
    session: requests.Session,
    source_grinder_id: str,
    target_grinder_id: str,
    source_settings: dict[str, int],
) -> dict[str, Any]:
    payload = {
        "sourceGrinderId": source_grinder_id,
        "targetGrinderId": target_grinder_id,
        "sourceSettings": source_settings,
    }
    response = post_json(session, CONVERT_ENDPOINT, payload, headers=JSON_HEADERS)
    return response


def build_reference_mapping_for_grinder(
    session: requests.Session,
    source_grinder: dict[str, Any],
    target_grinder: dict[str, Any],
    sample_count: int = 101,
) -> dict[str, Any]:
    source_math = build_grinder_math(source_grinder)
    target_math = build_grinder_math(target_grinder)

    points = evenly_spaced_ints(
        source_math.flat_start,
        source_math.flat_end,
        min(sample_count, source_math.total_positions),
    )

    mappings: list[dict[str, Any]] = []
    failures: list[dict[str, Any]] = []

    for flat in points:
        source_values = unflatten_values(flat, source_math.bases)
        source_payload = dict_from_units(source_math.units, source_values)

        try:
            response = convert_settings(
                session=session,
                source_grinder_id=source_math.id,
                target_grinder_id=target_math.id,
                source_settings=source_payload,
            )
        except Exception as exc:  # noqa: BLE001
            failures.append(
                {
                    "source_flat": flat,
                    "source_setting": source_payload,
                    "error": str(exc),
                }
            )
            continue

        if response.get("error"):
            failures.append(
                {
                    "source_flat": flat,
                    "source_setting": source_payload,
                    "error": response["error"],
                }
            )
            continue

        target_values = values_from_response_units(target_math.units, response)
        target_flat_raw = flatten_values(target_values, target_math.bases)
        target_flat_clamped = min(max(target_flat_raw, target_math.flat_start), target_math.flat_end)
        target_values_clamped = unflatten_values(target_flat_clamped, target_math.bases)

        mappings.append(
            {
                "source_flat": flat,
                "source_setting": source_payload,
                "target_setting_raw": dict_from_units(target_math.units, target_values),
                "target_setting_clamped": dict_from_units(target_math.units, target_values_clamped),
                "target_flat_raw": target_flat_raw,
                "target_flat_clamped": target_flat_clamped,
            }
        )
        time.sleep(0.03)

    return {
        "source_grinder_id": source_math.id,
        "target_grinder_id": target_math.id,
        "sample_count_requested": min(sample_count, source_math.total_positions),
        "sample_count_completed": len(mappings),
        "sample_count_failed": len(failures),
        "source_range_flat": [source_math.flat_start, source_math.flat_end],
        "target_range_flat": [target_math.flat_start, target_math.flat_end],
        "mappings": mappings,
        "failures": failures,
    }


def build_reference_conversion_dataset(
    session: requests.Session,
    grinders: list[dict[str, Any]],
    sample_count: int = 101,
) -> dict[str, Any]:
    by_id = {g["id"]: g for g in grinders}
    reference = by_id.get(REFERENCE_GRINDER_ID)
    if not reference:
        raise RuntimeError(f"Reference grinder '{REFERENCE_GRINDER_ID}' not found in widget API.")

    free_grinders = [g for g in grinders if str(g.get("tier", "")).lower() == "free"]
    free_grinders_sorted = sorted(free_grinders, key=lambda g: g["id"])

    grinder_entries: list[dict[str, Any]] = []
    for grinder in free_grinders_sorted:
        source_id = grinder["id"]

        preview_to_ref = preview_mode(session, source_id, REFERENCE_GRINDER_ID)
        preview_from_ref = preview_mode(session, REFERENCE_GRINDER_ID, source_id)

        is_preview_limited = bool(preview_to_ref.get("isPreviewMode")) or bool(
            preview_from_ref.get("isPreviewMode")
        )

        entry: dict[str, Any] = {
            "grinder_id": source_id,
            "grinder_name": grinder.get("name"),
            "tier": grinder.get("tier"),
            "preview_check_to_reference": preview_to_ref,
            "preview_check_from_reference": preview_from_ref,
            "is_preview_limited": is_preview_limited,
            "to_reference": None,
            "from_reference": None,
            "confidence": "high" if not is_preview_limited else "low",
        }

        if not is_preview_limited:
            entry["to_reference"] = build_reference_mapping_for_grinder(
                session=session,
                source_grinder=grinder,
                target_grinder=reference,
                sample_count=sample_count,
            )

            entry["from_reference"] = build_reference_mapping_for_grinder(
                session=session,
                source_grinder=reference,
                target_grinder=grinder,
                sample_count=sample_count,
            )

            to_failed = entry["to_reference"]["sample_count_failed"]
            from_failed = entry["from_reference"]["sample_count_failed"]
            if to_failed > 0 or from_failed > 0:
                entry["confidence"] = "medium"

        grinder_entries.append(entry)

    return {
        "reference_grinder_id": REFERENCE_GRINDER_ID,
        "grinders_included": len(grinder_entries),
        "sample_count_per_direction": sample_count,
        "grinders": grinder_entries,
    }


def build_combined_dataset(
    grinders: list[dict[str, Any]],
    pages: list[dict[str, Any]],
    reference_conversion: dict[str, Any],
) -> dict[str, Any]:
    methods: dict[str, dict[str, Any]] = {}
    for page in pages:
        for m in page.get("method_ranges", []):
            key = m["method"].strip().lower()
            entry = methods.setdefault(
                key,
                {
                    "method": m["method"],
                    "observations": [],
                },
            )
            entry["observations"].append(
                {
                    "min_microns": m["min_microns"],
                    "max_microns": m["max_microns"],
                    "source_url": page["url"],
                }
            )

    method_summary: list[dict[str, Any]] = []
    for key, entry in sorted(methods.items(), key=lambda x: x[0]):
        mins = [obs["min_microns"] for obs in entry["observations"]]
        maxs = [obs["max_microns"] for obs in entry["observations"]]
        method_summary.append(
            {
                "method": entry["method"],
                "min_microns_observed": min(mins),
                "max_microns_observed": max(maxs),
                "median_min_microns": float(sorted(mins)[len(mins) // 2]),
                "median_max_microns": float(sorted(maxs)[len(maxs) // 2]),
                "observation_count": len(entry["observations"]),
                "sources": [obs["source_url"] for obs in entry["observations"]],
            }
        )

    return {
        "generated_at_utc": now_utc_iso(),
        "sources": {
            "widget_api_grinders": GRINDERS_ENDPOINT,
            "widget_api_convert": CONVERT_ENDPOINT,
            "widget_api_preview": PREVIEW_ENDPOINT,
            "page_sitemap": PAGE_SITEMAP,
            "site_root": BEEAN_SITE,
        },
        "notes": [
            "Reference conversion mappings are high-confidence only for grinder pairs without preview mode.",
            "In public mode, Beean marks most Pro grinders as preview-limited; those should be treated as low-confidence.",
            "Method micron ranges come from Beean grind-settings FAQ text extraction and are approximate starting points.",
        ],
        "counts": {
            "widget_grinders_total": len(grinders),
            "grind_settings_pages_total": len(pages),
            "reference_grinders_total": reference_conversion.get("grinders_included", 0),
        },
        "method_micron_summary": method_summary,
    }


def write_json(path: Path, payload: Any) -> None:
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="Build grinder conversion dataset from scraped web sources.")
    parser.add_argument(
        "--output-dir",
        default=str(
            Path(__file__).resolve().parents[2]
            / "src"
            / "main"
            / "resources"
            / "grinder_conversion"
            / "data"
        ),
        help="Directory where JSON artifacts will be written.",
    )
    parser.add_argument(
        "--sample-count",
        type=int,
        default=101,
        help="Sample points per grinder direction for reference conversion mappings.",
    )
    args = parser.parse_args()

    output_dir = Path(args.output_dir).resolve()
    ensure_dir(output_dir)

    with requests.Session() as session:
        grinders = get_json(session, GRINDERS_ENDPOINT, headers=REQUEST_HEADERS)
        pages = scrape_grind_settings_pages(session)
        reference_conversion = build_reference_conversion_dataset(
            session=session,
            grinders=grinders,
            sample_count=args.sample_count,
        )
        combined = build_combined_dataset(grinders, pages, reference_conversion)

    write_json(output_dir / "beean_widget_grinders.json", grinders)
    write_json(output_dir / "beean_grind_settings_pages.json", pages)
    write_json(output_dir / "free_grinders_reference_conversion.json", reference_conversion)
    write_json(output_dir / "grinder_conversion_dataset.json", combined)

    print(f"Wrote dataset files to: {output_dir}")
    print(f"- beean_widget_grinders.json ({len(grinders)} grinders)")
    print(f"- beean_grind_settings_pages.json ({len(pages)} pages)")
    print(
        "- free_grinders_reference_conversion.json "
        f"({reference_conversion.get('grinders_included', 0)} free grinders)"
    )
    print("- grinder_conversion_dataset.json (summary + methodology metadata)")


if __name__ == "__main__":
    main()
