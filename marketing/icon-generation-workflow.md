# App Icon Generation — Workflow with `opw_audio_project`

Script Gemini đã tích hợp sẵn trong `opw_audio_project` (cùng setup TTS). Dùng Nano Banana (`gemini-2.5-flash-image`) để generate icon variants.

## Setup (1 lần)

```bash
cd /Volumes/Entertainment/GeminiGenerator/opw_audio_project
source venv/bin/activate
```

Đã có `.env` với `GEMINI_API_KEY` từ TTS setup. Không cần config thêm.

## Cách dùng

### 1. Xem 8 concept prompts

```bash
python scripts/generate_app_icon.py --list
```

### 2. Dry-run (in prompt, không call API)

```bash
python scripts/generate_app_icon.py --concept 1 --variants 2 --dry-run
```

### 3. Generate 1 concept × 4 variants

```bash
python scripts/generate_app_icon.py --concept 1
```

Output: `output/app_icon/{timestamp}_batch/concept_01_bee_letter_a_var_{1-4}.png`

### 4. Generate tất cả 8 concepts × 4 variants = 32 outputs

```bash
python scripts/generate_app_icon.py
```

Free tier: ~13 phút (sleep 25s giữa calls).
Paid tier: `OPW_TIER=paid python scripts/generate_app_icon.py` — ~1 phút.

### 5. Refine icon đã chọn (iterate)

Pick 1 variant ưng → refine bằng edit prompt:

```bash
python scripts/generate_app_icon.py \
    --refine output/app_icon/2026-05-15_22-30_batch/concept_01_bee_letter_a_var_2.png \
    --edit-prompt "Make the bee 20% larger, brighter yellow, add subtle glow around letter A" \
    --variants 3
```

Nano Banana sẽ dùng PNG đó làm reference image → giữ composition + style, chỉ thay đổi theo edit prompt.

## 8 Concept đã setup sẵn (sync với marketing/iconikai-prompts.md)

| ID | Name | Concept |
|---|---|---|
| 1 | bee_letter_a | Bee mascot + Letter A (PRIMARY) |
| 2 | open_book_abc | Open book + 3D ABC letters |
| 3 | bee_honeycomb_letters | Bee + honeycomb hexagons với letters |
| 4 | mouth_speech_bubble | Mouth speaking letter A trong speech bubble |
| 5 | three_mascot_trio | 3 mascots (bee + bear + cat) + letter A |
| 6 | big_letter_a_bee | Big letter A + tiny bee perch |
| 7 | star_letters_reward | ABC letters + gold star |
| 8 | headphones_abc_audio | Headphones wrapping ABC letters |

Edit prompts trong `scripts/generate_app_icon.py` (ICON_CONCEPTS list) nếu muốn thêm/sửa.

## Brand constraints auto-applied

Mọi prompt tự động kèm:
- Rounded squircle format
- 1024×1024 square, 10% safe-zone padding
- Flat design, subtle shadow
- NO text/words (except letter A nếu prompt mention)
- Kid-safe cartoon style
- Brand palette: `#FFD93D` yellow, `#6BCB77` mint, `#4D96FF` sky blue
- Negative hints: avoid realistic photo, dark, 3D, chalkboard, etc.

## Output structure

```
output/app_icon/
└── 2026-05-15_22-30-15_batch/
    ├── concept_01_bee_letter_a_var_1.png
    ├── concept_01_bee_letter_a_var_2.png
    ├── ...
    ├── concept_08_headphones_abc_audio_var_4.png
    └── metadata.json          # log prompt + status mỗi variant
```

## Workflow đầy đủ — Generate → Pick → Adapt

**Step 1 — Generate batch** (~13 min free tier):
```bash
python scripts/generate_app_icon.py
```

**Step 2 — Pick 3-5 best** (10 min manual):
- Mở folder output → loại bỏ variants không ưng
- Tiêu chí: readable ở 48px, no text, dominant yellow, distinguishable từ competitor

**Step 3 — Refine top pick** (~5 min, optional):
```bash
python scripts/generate_app_icon.py \
    --refine output/app_icon/{your_pick}.png \
    --edit-prompt "Your refinement"
```

**Step 4 — Adapt thành icon platform-ready** (~10 min):
- Open https://icon.kitchen/
- Upload final PNG
- Download zip với:
  - Android adaptive XML (`mipmap-anydpi-v26/`)
  - All Android sizes (`mipmap-xxxhdpi/`, etc.)
  - All iOS sizes
- Drop files vào project:
  - Android: `composeApp/src/androidMain/res/mipmap-*/`
  - iOS: `composeApp/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/`

**Total**: $0, ~30 min (free tier).

## Cost estimation

| Tier | Per icon batch (32 outputs) | Per refine (3 outputs) |
|---|---|---|
| Free tier (Nano Banana free quota) | $0 | $0 |
| Paid tier (Imagen 4 Fast $0.02/img) | $0.64 | $0.06 |

Nano Banana free quota cho phép vài chục batches/day → free tier dư sức.

## Customize prompts

Edit file `scripts/generate_app_icon.py` → `ICON_CONCEPTS` list:
```python
ICON_CONCEPTS = [
    {
        "id": 1,
        "name": "bee_letter_a",
        "prompt": "Your custom prompt here...",
    },
    # ...
]
```

Re-run script — new prompts tự động pick up.
