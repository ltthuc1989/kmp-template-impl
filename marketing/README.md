# Marketing Folder — ABC Phonics Kids

Per-locale store listings, brand assets, and asset production pipeline.

## Structure

```
marketing/
├── README.md                          (this file)
├── brand-guidelines.md                Brand colors, typography, mascots, naming
├── asset-pipeline.md                  Tools + workflow for creating icon/screenshots/video
├── keywords-tracker.md                Weekly keyword rank tracking template
└── store-listing/
    ├── vi-VN/listing.md               🇻🇳 Vietnam (PRIMARY)
    ├── id-ID/listing.md               🇮🇩 Indonesia
    ├── th-TH/listing.md               🇹🇭 Thailand
    ├── pt-BR/listing.md               🇧🇷 Brazil
    ├── es-MX/listing.md               🇲🇽 Mexico
    ├── ja-JP/listing.md               🇯🇵 Japan (App Store priority)
    ├── ko-KR/listing.md               🇰🇷 Korea
    ├── tr-TR/listing.md               🇹🇷 Turkey
    └── en-US/listing.md               🌍 EN default (PASSIVE — fallback)
```

## Launch order

1. **Week 0 (vi-VN)** — copy `store-listing/vi-VN/listing.md` → Play Console VN locale
2. **Week +1 (vi-VN App Store)** — same to App Store Connect VN
3. **Week +2** (id-ID, th-TH) — submit Indonesia + Thailand
4. **Week +3** (pt-BR, es-MX) — submit Brazil + Mexico
5. **Week +4** (ja-JP) — submit Japan (App Store priority)
6. **Week +5** (ko-KR, tr-TR) — submit Korea + Turkey
7. **Week +6** (en-US passive) — submit as global fallback

## Each `listing.md` contains

- App title (≤ 30 chars)
- Short description (≤ 80 chars)
- Long description (3500-4000 chars, TF-IDF optimized)
- Target keywords with rank goals
- ⭐ markers = pure-phonics keyword (low comp, high brand fit)

## Asset production

See [asset-pipeline.md](asset-pipeline.md):
- Total budget: ~$40
- Total time: ~24 hours core + 8 hrs/locale for video
- Tools: IconikAI, Figma, Screenshots Pro, CapCut, ElevenLabs

## Brand consistency

See [brand-guidelines.md](brand-guidelines.md) — colors, mascots, tone, trademark avoidance.

## Iteration

After launch, update [keywords-tracker.md](keywords-tracker.md) weekly. Drop keywords stuck at NR > 200 after 14 days, replace with new long-tail from Play Console Search Terms report.
