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

## Backend API (Implemented)

The backend now exposes:

1. `GET /api/grinder-conversion/grinders`
- returns supported grinder catalog for FE selectors
- includes `id`, `name`, `make`, `model`, `tier`, and `units[]`

2. `POST /api/grinder-conversion/convert`
- request body:
  - `sourceGrinderId`
  - `targetGrinderId`
  - `sourceSetting` (`rotation`, `number`, `click`)
- response includes:
  - normalized/clamped `sourceSetting`
  - converted `targetSetting`
  - `sourceFlat`, `targetFlat`, `referenceFlatEstimated`, `confidence`

FE integration notes:
1. Treat missing source fields as optional; backend defaults missing values to `0`.
2. Always use returned `sourceSetting` to update FE state after conversion, since backend clamps out-of-range inputs.
3. Render input fields dynamically from `units` returned by `GET /grinders`.

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

1. Current committed conversion dataset is intentionally scoped to the reference file coverage (`free_grinders_reference_conversion.json`, 41 grinders).
2. Conversion output is deterministic for the committed dataset, but real-world grinding is still approximate (calibration zero-point, burr wear, bean density, roast level, humidity).
3. If you expand the dataset, rebuild and validate the files before shipping (`build_grinder_conversion_dataset.py`).
