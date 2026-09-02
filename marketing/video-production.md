# Promo video Play Store + video marketing — kế hoạch sản xuất

> **Trạng thái: HOÃN CÓ CHỦ ĐÍCH (22/08/2026). Chưa quay, chưa dựng, chưa có script.**
> Đây là bản kế hoạch đã nghiên cứu xong, để mở ra làm khi cổng khởi động ở [§0](#0-cổng-khởi-động)
> mở. Cố tình **không** viết sẵn script — mục 4 của [asset-pipeline.md](asset-pipeline.md) đã cho
> thấy một pipeline viết trước rồi không chạy sẽ mục ruỗng và nói dối trong im lặng.

Doc này thay thế mục 4 của [asset-pipeline.md](asset-pipeline.md) (mục đó còn 3 chỉ dẫn sai, xem [§9](#9-bẫy-đã-biết)).

---

## 0. Cổng khởi động

App đã live (`com.beely.phonicskids`) với 7 ảnh chụp màn hình + feature graphic, **chưa có video**.
Ô video là chỗ trống cuối cùng của trang store — nhưng chưa phải lúc lấp, vì nội dung chưa xong:

| Cấp | Nội dung | Audio | Quay được chưa |
|---|---|---|---|
| L1 — The Alphabet | ✅ 8 unit / 26 bài | ✅ | ✅ |
| L2 — Short Vowels | ✅ 8 unit / 24 bài | ✅ | ✅ |
| L3 — Long Vowels | ✅ chữ + tranh xong | ❌ **chưa có** — mọi bước im tiếng (`LevelRepository.kt:32`) | ❌ |
| L4 — Blends & Digraphs | curriculum có, chưa mở | ❌ | ❌ "Coming soon" |
| L5 — Letter Combinations | curriculum có, chưa mở | ❌ | ❌ "Coming soon" |

`LAUNCHED_PREMIUM_LEVELS = setOf("L2", "L3")` — L4/L5 vẫn khoá.

**Mở cổng khi cả 3 điều kiện đúng:**

1. L4 + L5 đã mở và **có audio** (không quay được màn hình im tiếng — nửa video là âm thanh).
2. Danh sách từ và số liệu marketing đã chốt (câu "5 cấp độ · 488+ từ" trong video phải khớp
   listing; đổi số sau khi quay nghĩa là quay lại).
3. Bản build sắp ship là bản dùng để quay — theo đúng luật Cổng 7 của
   [APP_DELIVERY_WORKFLOW.md](../docs/engineering/APP_DELIVERY_WORKFLOW.md): asset lấy từ bản
   sắp ship, không phải bản cũ.

Trong lúc chờ, phần đã nghiên cứu xong nằm ở §1–§9 — mở ra là làm được ngay, không phải điều tra lại.

---

## 1. Đối thủ làm thế nào — số liệu thật

Quét trang Play của 14 app phonics/kids, lọc video của **chính app** (video đếm ≥2 lần trong HTML;
video đếm 1 lần là carousel "app tương tự"), rồi tải sprite storyboard của YouTube để **xem từng
khung hình thật**, không đoán qua tiêu đề.

Cách lặp lại kiểm tra:

```bash
curl -sL -A "Mozilla/5.0" "https://play.google.com/store/apps/details?id=<pkg>&hl=en_US" \
  | grep -o 'youtube\.com/embed/[A-Za-z0-9_-]\{11\}' | sed 's|.*/||' | sort | uniq -c
```

| App | Video | Dài | Năm | Kiểu dựng (nhìn từ khung hình) |
|---|---|---|---|---|
| Khan Academy Kids | ❌ | | | không có video |
| Duolingo ABC | ❌ | | | không có video |
| Reading.com | ❌ | | | không có video |
| ABCmouse | ❌ | | | không có video |
| Hooked on Phonics | ❌ | | | không có video |
| RV AppStudios — ABC Kids | `MuBvRlbNCVM` | 30s | 2024 | **footage app thô 100%** — không caption, không khung máy |
| Reading Eggs | `un8P0d756gI` | 30s | 2022 | footage app + **banner cong hồng dưới đáy**, mỗi cắt một câu lợi ích |
| Bebi Family | `h4Jv7fKLlUg` | 30s | 2022 | **title card 3s** ("Play and Learn With Us — 30+ Million Kids") rồi footage app |
| Endless Alphabet | `eYl0bLO26Ig` | 73s | 2013 | logo động + diễu hành mascot vẽ tay → footage app có **ngón tay cutout** |
| Teach Your Monster | `yVnQiVYbPGI` | 84s | 2019 | **thế giới hoạt hình riêng**, kinetic type, footage app đặt **trong laptop vẽ minh hoạ** |
| Read with Phonics | `mteJBoynHUA` | 19s | 2017 | hoạt hình 2D thuần (UFO ngoài vũ trụ) — **không thấy UI app** |
| Phonics Hero | `ZDj7-GHVVf8` | 91s | 2011 | dài lê thê, kiểu video hướng dẫn — phản ví dụ |
| UptoSix Phonics | `iamWq0TVd0o` | 374s | 2025 | 6 phút trên trang store — phản ví dụ |

**Ba kết luận:**

1. **5/14 app không có video**, kể cả Khan Academy Kids và Duolingo ABC — hai app mạnh nhất mảng
   này. Video là lợi thế, không phải điều kiện sống còn → hoãn thêm vài tháng không mất gì, và
   không đáng thuê agency (giá thị trường **$2.000–8.000** cho 30 giây).
2. **Không app nào bịa UI.** Kể cả bản công phu nhất (Teach Your Monster) vẫn là footage app thật,
   chỉ khác là nó **nằm trong một cảnh vẽ**. Cái duy nhất thuần hoạt hình (Read with Phonics)
   cũng là cái cũ nhất và mỏng nhất.
3. **Cảm giác "được đặt hàng studio" đến từ 5 lớp vỏ**, không phải từ footage:
   plate minh hoạ làm nền · dải lợi ích cố định · title & end card có thương hiệu ·
   chuyển động liên tục (không khung nào đứng yên) · thiết kế âm thanh.

→ Nhận xét ban đầu ("họ custom chứ không quay app") đúng ở phần **vỏ**, sai ở phần **ruột**.
Kế hoạch dưới đây làm vỏ custom, giữ ruột là app thật.

## 2. Luật Google — hai câu đổi hẳn cách dựng

Trang [content quality guidelines](https://support.google.com/googleplay/android-developer/answer/12929944) viết nguyên văn:

> "Use captured footage of the app or game itself. **Don't include people interacting with the
> device** (for example, fingers tapping on the device)"

> "avoid using generic video that only describes the title but not the promotional content"

Nghĩa là **không quay ngón tay chạm màn hình** (đừng bắt chước Endless Alphabet, và bỏ cảnh mở đầu
"close-up of kid hand tapping screen" trong doc cũ), và **không làm phim hoạt hình thuần** không
thấy app. Hướng ở §3 nằm đúng giữa hai lằn ranh này.

Điều kiện kỹ thuật (kiểm chứng 22/08/2026):

| | |
|---|---|
| Nộp bằng | URL YouTube đầy đủ — không `youtu.be`, không playlist, không kênh |
| Điều kiện hiện | listing phải có feature graphic — ✅ đã có |
| Autoplay | 30 giây đầu, **chạy tắt tiếng** |
| Riêng tư | trang preview-assets nói *public hoặc unlisted*; trang YouTube-on-Play nói *phải public* → chọn **Public** cho chắc |
| Monetization | **tắt** (có ads chèn là video không hiện) · Embedding: **bật** · không age-restricted |
| Shorts / live | **không hỗ trợ** → master phải **16:9 landscape** |
| Sở hữu | video phải thuộc kênh của app → **cần lập kênh YouTube Beely** (chưa có) |
| Made for kids | bắt buộc set (COPPA). Vẫn embed được; mất comment + save-to-playlist |
| Dài | 25–30s |

> ⚠️ **Bẫy Shorts**: YouTube tự xếp mọi video **dọc và ≤3 phút** thành Short. Một promo dọc 30 giây
> vì thế thành Short → Play không nhận. Đó là lý do master phải là 16:9, không phải 9:16.

Nguồn: [Add preview assets](https://support.google.com/googleplay/android-developer/answer/9866151) ·
[Showcase your app with YouTube videos](https://support.google.com/googleplay/android-developer/answer/15501235) ·
[YouTube made-for-kids FAQ](https://support.google.com/youtube/answer/9684541).

## 3. Hướng dựng — "vỏ custom, ruột app thật"

Điểm mấu chốt: **phần đắt nhất của một video custom thì dự án đã sở hữu sẵn.**

| Tài sản | Ở đâu | Dùng làm gì |
|---|---|---|
| 64 tranh truyện màu nước **bản landscape** | `$OPW_ROOT/output/level_{1,2}/stories/*/images/scene_0*_landscape.png` | plate nền — đúng thứ Teach Your Monster phải thuê vẽ |
| Bản portrait shipped trong app | `core/resource/src/commonMain/composeResources/files/images/level_*/stories/` | plate cho bản cắt dọc |
| Pipeline sinh ảnh AI | `$OPW_ROOT/scripts/generate_feature_graphic.py`, `generate_app_icon.py` | sinh plate hook/end đúng nét vẽ |
| Audio TTS studio | `core/resource/.../files/audio/phonemes/*.mp3`, `.../files/sfx/*.mp3`, `$OPW_ROOT/output/level_*` | track sạch — **không thu tiếng emulator** |
| Icon + mascot | `marketing/assets/icons/app_icon_transparent.png` | bumper mở/đóng, end card |
| Font có tiếng Việt | `marketing/assets/fonts/Quicksand-Variable.ttf` | banner + kinetic type |

→ Video custom với chi phí thêm **≈ $0–5**, thay vì $2.000–8.000.

**Ngôn ngữ hình ảnh** (mọi khung hình phải có ít nhất một lớp thương hiệu):

- **Plate**: tranh màu nước landscape, parallax chậm 4–6% (không khung nào đứng yên).
- **Screen card**: clip app bo góc ~32px, đổ bóng mềm, đặt lệch về phía trống của plate.
  **Không dùng khung điện thoại ảnh thật** — Google khuyên tối giản phần không phải app.
- **Dải lợi ích**: pill vàng `#FFD93D` gần đáy, đổi câu mỗi cắt — vai trò giống banner cong của
  Reading Eggs, và chính nó là thứ khiến footage hết giống bản quay màn hình thô.
- **Bumper**: mascot bay ngang khi chuyển cảnh; end card = icon + tagline + Play badge.
- **Cấm**: ngón tay trong khung (luật Google), mặt trẻ em ([brand-guidelines.md](brand-guidelines.md)).

**Plate shortlist đã soi sẵn** (chọn theo vùng trống để đặt screen card):

| Plate | Nguồn (dưới `$OPW_ROOT/output/`) | Card đặt |
|---|---|---|
| `hook` | `level_1/stories/L1_S03_a_gift/images/scene_02_problem_landscape.png` — nền gần như trắng, thoáng nhất | — (title card) |
| `farm` | `level_1/stories/L1_S02_egg_on_the_farm/images/scene_01_intro_landscape.png` | phải |
| `beach` | `level_1/stories/L1_S07_sun_and_rain/images/scene_01_intro_landscape.png` | phải |
| `jungle` | `level_1/stories/L1_S05_monkeys_nut/images/scene_03_solution_landscape.png` | trái |
| `bedroom` | `level_1/stories/L1_S01_apple_ant_cat/images/scene_01_intro_landscape.png` | trái |
| `zoo` | `level_1/stories/L1_S08_at_the_zoo/images/scene_01_intro_landscape.png` | phải |
| `endcard` | `level_1/stories/L1_S04_king_and_kite/images/scene_03_solution_landscape.png` — trời rộng, chủ thể nhỏ | — (end card) |

Bản landscape **chỉ có ngoài repo** (ổ ngoài). Khi khởi động thì copy + crop về 1920×1080 JPEG rồi
commit, để dựng lại được trên máy khác mà không cần gắn ổ.

> ⚠️ **Lệch thương hiệu phải chốt trước khi dựng**: [brand-guidelines.md](brand-guidelines.md) ghi
> mascot chính là **con ong 🐝**, nhưng icon đang live là **con kiến xanh cầm bút chì**. Video theo
> cái đang live; nên sửa brand doc cho khớp.

## 4. Storyboard 28s (bản nháp — chốt lại khi L4/L5 xong)

| Giây | Plate | Footage app | Dải chữ | Phụ thuộc |
|---|---|---|---|---|
| 0–3 | `hook` | — (mascot bay vào) | **"Bé đọc được tiếng Anh"** / "Kids learn to read English" | — |
| 3–8 | `farm` | Sound intro chữ **A** + sóng âm (L1 U1 bài 1, step 0) | "26 chữ cái · phát âm chuẩn bản ngữ" | L1 ✅ |
| 8–12 | `beach` | Tracing chấm điểm nét | "Tô chữ, chấm điểm từng nét" | L1 ✅ |
| 12–17 | `jungle` | Blending `a + g = ag` → `b + ag = bag` (L2 U2 — trong vùng free) | "Ghép vần, không học vẹt" | L2 ✅ |
| 17–21 | `bedroom` | Story karaoke — chữ sáng theo giọng kể | "8 truyện, chữ sáng theo giọng đọc" | L1 ✅ |
| 21–25 | `zoo` | 1 mini-game + Lesson Complete | "6 mini-game sau mỗi bài" | L1 ✅ |
| 25–28 | `endcard` | icon + Play badge | "Miễn phí 2 unit mỗi cấp · Không quảng cáo · Chơi offline" | **cần L4/L5 để nói "5 cấp độ"** |

Ba câu ở end card đã đối chiếu code: `FREE_UNITS_PER_LEVEL = 2`; ad composable là no-op stub
(`core/ui/.../ads/BottomBannerAd.kt`); listing live đang quảng cáo "works completely offline".

Luật cắt: footage app chiếm ≥60% khung từ giây thứ 3 · nhạc + SFX thật của app ·
VO nói với **phụ huynh**, không nói với trẻ · 3 giây đầu phải hiểu được khi **tắt tiếng**.

## 5. Tool stack (đã lọc theo máy này)

| Việc | Chọn | Giá | Ghi chú |
|---|---|---|---|
| Quay app | `adb shell screenrecord` (đã có) | $0 | reset `wm size`/`density` trước — bẫy đã dẫm ở screenshot |
| — mượt hơn | Android Studio → Running Devices → Record, hoặc `brew install scrcpy` | $0 | |
| Plate art | tranh story sẵn có + script sinh thêm | ~$0 | giữ nguyên nét watercolor |
| **Plate động** (nâng hạng "custom" rõ nhất) | **Veo 3.1 Fast/Lite** image-to-video qua Gemini API (billing đã bật) | ~$0,05–0,10/giây → 4 plate × 5s ≈ **$1–2** | chuyển động rất nhẹ, giữ nguyên tranh |
| Ghép plate + card + dải chữ | **ffmpeg** (đã có) | $0 | phần fiddly nhất — làm bằng script thì lặp lại được |
| Dựng cuối, kinetic type | **CapCut Desktop** hoặc **Jitter** (free tier) | $0 | 2–3h cho bản đầu |
| Voice-over VN + EN | ElevenLabs free tier, hoặc Cloud TTS qua pipeline `opw` | ~$0 | giọng người lớn, ấm |
| Nhạc nền | YouTube Audio Library / Pixabay / Mixkit | $0 | Play cấm nhạc không có quyền |

**Không cần**: agency ($2k–8k/30s) · Rotato / khung máy 3D (Google khuyên tối giản chrome) ·
Screenshots Pro, AppLaunchpad (tool ảnh, không phải video) · dịch vụ "AI app promo video" trả phí.

## 6. Bộ script sẽ dựng khi mở cổng

Cố tình chưa viết. Spec để lúc đó khỏi thiết kế lại:

| File | Việc |
|---|---|
| `marketing/assets/video/capture.sh` | `adb shell wm size reset && adb shell wm density reset` → `screenrecord --bit-rate 16M` → pull → ffmpeg chuẩn hoá CFR 30fps, bỏ audio |
| `marketing/assets/video/make_overlay.py` | render dải chữ / title card ra PNG trong suốt (PIL + Quicksand) |
| `marketing/assets/video/compose_shot.sh` | ffmpeg: plate (zoompan) + screen card (bo góc `geq` + bóng `boxblur`) + overlay PNG → 1 shot 1920×1080; `--target vertical` cho 1080×1920 |
| `marketing/assets/video/assemble.sh` | nối shot + trộn VO/nhạc → master |
| `$OPW_ROOT/scripts/generate_promo_plates.py` | sinh plate hook/end bằng Nano Banana, mirror `generate_feature_graphic.py` |

**Hai phát hiện về môi trường, đã kiểm, đừng dò lại:**

- `ffmpeg` 8.1 trên máy này **không có `drawtext`** (build thiếu libfreetype —
  `ffmpeg -filters | grep drawtext` không ra gì). Nên chữ phải render bằng PIL ra PNG rồi `overlay`.
- `python3` của homebrew **không có Pillow**, nhưng **`/usr/bin/python3` có sẵn Pillow 11.3.0** →
  chạy script ảnh bằng `/usr/bin/python3`, khỏi lập venv.

## 7. Video marketing (khác video store)

| | Store (Play) | Ads: TikTok / Reels / Shorts / Google Ads |
|---|---|---|
| Tỉ lệ | 16:9 · 1920×1080 | 9:16 · 1080×1920 (+ 1:1 cho Google Ads) |
| Dài | 25–28s | 6s / 15s cutdown |
| Hook | mở bằng giá trị | 1 cảnh gây tò mò trong 1,5s đầu (`b + ag` → `bag` rồi hiện con vật) |
| Chữ | dải lợi ích | chữ to cháy hình, chừa 15% trên / 20% dưới cho UI nền tảng |
| Nhắm | — | **phụ huynh**; không personalized ads tới trẻ (COPPA) |

Cùng bộ plate + footage, chỉ đổi khung và nhịp cắt. Bản dọc **không** dùng cho listing Play
(sẽ thành Short) — chỉ chạy quảng cáo.

## 8. QA + upload

Trước khi nộp:

- [ ] Tạm dừng ở 5 mốc bất kỳ — mọi khung có ít nhất 1 lớp thương hiệu, không khung nào là ảnh
      chụp màn hình trần (đây là bài test "có custom hay không")
- [ ] Tắt tiếng, thu cửa sổ còn ~50% → 3 giây đầu vẫn hiểu
- [ ] Không ngón tay, không mặt trẻ em, không lộ build debug / status bar lệch giờ
- [ ] Số liệu trong video khớp listing live (`fetch-live.py` để đối chiếu)
- [ ] Nhạc có quyền dùng · VN không lỗi dấu
- [ ] `ffprobe`: 1920×1080, 25–28s, H.264

Upload: kênh Beely → **Unlisted** để thử → dán URL vào Play Console (bản nháp) xem Console có nhận
không → nếu báo lỗi Shorts/ads/embed thì sửa trong YouTube Studio → chuyển **Public** + Made for
kids → submit cùng bản release kế → chạy `python3 marketing/store-listing/fetch-live.py` để chụp
lại listing.

## 9. Bẫy đã biết

| Bẫy | Thực tế |
|---|---|
| Export dọc 1080×1920 cho listing | YouTube xếp thành Short → Play không nhận. Phải 16:9 |
| "Upload unlisted là xong" | Hai trang help của Google mâu thuẫn → chọn Public |
| Mở đầu bằng tay bé chạm màn hình | Google nói thẳng đừng làm |
| Làm phim hoạt hình cho sang | Google gọi là "generic video"; app duy nhất làm vậy cũng là app yếu nhất trong mẫu |
| Quay khi chưa reset `wm size`/`density` | ra 1080×2400 @450 thay vì 1080×2424 @420 — lệch bộ asset đang live |
| Cố thu tiếng emulator | không cần: audio app vốn là file TTS studio, ghép thẳng vào bản dựng |
| Quay bản build cũ | Cổng 7 bắt buộc quay từ bản sắp ship |

Ba dòng đầu là 3 chỗ sai còn nằm trong mục 4 của [asset-pipeline.md](asset-pipeline.md).
