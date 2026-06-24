# Publish-ready store assets

Generated for Play Console / App Store upload. All files meet store spec.

## icon/
- `play-store-icon-512.png` — **512×512**, 32-bit PNG (RGBA), ~138 KB (≤ 1 MB).
  Source: `../icons/app_icon_launcher.png` (matches in-app `ic_launcher`).
  Full-square art; Play applies its own rounded mask.

## feature-graphic/
- `feature-graphic-<locale>.png` — **1024×500** PNG, ~416 KB each (≤ 15 MB).
  Source: `../output/feature_graphic_final_v2/`.
  Locales: en, vi, id, th, pt-BR, es-MX, ja, ko, tr.

Upload the locale matching each store listing; use `en` as global fallback.
