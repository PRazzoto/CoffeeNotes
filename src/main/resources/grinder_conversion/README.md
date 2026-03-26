# Grinder Conversion Research Dataset

This folder contains a scraped + normalized dataset to support a grinder-setting converter similar to:

- https://beeancoffee.com/grinder-setting-converter/

Generated on: `2026-03-25` (UTC)

## What Was Collected

1. Beean widget API grinder catalog
- Endpoint: `https://converter.beeancoffee.com/api/widget/v1/grinders`
- Result: 113 grinder profiles (41 free, 72 Pro), including unit system and setting ranges.

2. Beean grind-settings pages
- Source index: `https://beeancoffee.com/page-sitemap.xml`
- Scraped pages: 64 URLs matching `*-grind-settings/`
- Extracted:
  - FAQ question/answer pairs
  - method micron ranges (when present)
  - microns-per-step candidates (when present)

3. Reference conversion mappings (high-confidence free grinders)
- Endpoints:
  - `https://converter.beeancoffee.com/api/widget/v1/preview-conversion`
  - `https://converter.beeancoffee.com/api/widget/v1/convert`
- Method:
  - Use `comandante_c40` as reference axis.
  - For each free grinder, sample up to 101 points for:
    - grinder -> reference
    - reference -> grinder
  - Store both mappings for interpolation-based conversion.

## Files

- `data/beean_widget_grinders.json`
  - Raw grinder catalog from widget API.

- `data/beean_grind_settings_pages.json`
  - Raw page extraction for grind-setting pages.

- `data/free_grinders_reference_conversion.json`
  - Core conversion dataset for free grinders, with per-grinder mappings to/from `comandante_c40`.

- `data/grinder_conversion_dataset.json`
  - Lightweight summary (source metadata, counts, method micron summary, notes).

- `scripts/grinder_conversion/build_grinder_conversion_dataset.py`
  - Rebuilds all JSON files above.

- `scripts/grinder_conversion/convert_with_reference_dataset.py`
  - Local/offline conversion helper using the generated reference mapping file.

## Quick Usage

Rebuild dataset:

```bash
python scripts/grinder_conversion/build_grinder_conversion_dataset.py --sample-count 101
```

Test an offline conversion:

```bash
python scripts/grinder_conversion/convert_with_reference_dataset.py ^
  --source baratza_encore ^
  --target comandante_c40 ^
  --setting click=20
```

## Recommended API Strategy

1. Accept `{sourceGrinderId, targetGrinderId, sourceSetting}`.
2. Convert source setting to a flattened numeric position.
3. Interpolate source -> reference using `to_reference.mappings`.
4. Interpolate reference -> target using `from_reference.mappings`.
5. Unflatten target position into target grinder unit fields.

This avoids direct pairwise storage for every grinder pair and keeps the model compact.

## Accuracy and Caveats

- Free grinders: high-confidence mappings (no preview mode in source API).
- Pro grinders: API is preview-limited in public mode; avoid treating those conversions as authoritative unless you obtain paid/full data access.
- Any grinder converter is approximate in practice because burr wear, zero-point calibration, coffee density, roast level, and humidity change outcomes.
