# Play Store Listing — Vietnam (vi-VN) — PRIMARY MARKET

⚠️ **Mốc 2 draft.** So với [live.md](live.md) trước khi paste vào Play Console.
Refresh snapshot trước: `python3 marketing/store-listing/fetch-live.py vi-VN`.

**Nguyên tắc gần-minimal-diff.** Không viết lại dòng nào cho vui. Chỉ đổi: 2 khối level
song song, các con số ship Level 2 làm sai, và 1 câu offline nói sai sự thật. `BÉ HỌC GÌ`
thu lại còn số liệu chung để 2 khối level song song nhau — có `LEVEL 2` mà không có
`LEVEL 1` phía trên thì đọc như thể thiếu mất Level 1. Các gạch đầu dòng của nó chuyển
xuống `LEVEL 1`, giữ nguyên chữ.

## App title (16 chars) — GIỮ NGUYÊN

```
ABC Phonics Kids
```

## Short description (69 chars) — ĐỔI

```
Phonics tiếng Anh cho bé 3-8 — A-Z, nguyên âm ngắn, 16 truyện, 6 game
```

## Long description — THÊM 1 khối, sửa số truyện, nói rõ dòng offline

```
Phonics Kids — học đọc tiếng Anh cho bé 3-8 tuổi qua phonics, không quảng cáo. 488+ từ vựng, 16 truyện, 6 mini-game.

Phonics Kids giúp bé 3-8 tuổi học đọc tiếng Anh qua phương pháp phonics. Bé học bảng chữ cái A-Z, ghép âm thành từ thật, đọc 16 câu chuyện — và chơi 6 mini-game vui sau mỗi bài.

🌟 BÉ HỌC GÌ
✓ 488+ từ vựng có audio phát âm
✓ 16 câu chuyện — chữ sáng theo lời đọc (karaoke style)

📖 LEVEL 1: BẢNG CHỮ CÁI
✓ Trọn 26 chữ A-Z với âm chuẩn bản xứ
✓ Dạy âm của chữ, không chỉ tên chữ — nền tảng để bé tự đọc
✓ Tô chữ thông minh — chấm điểm độ chính xác nét vẽ
✓ 8 truyện xây từ chính những chữ bé vừa học

📖 LEVEL 2: NGUYÊN ÂM NGẮN
✓ Nguyên âm ngắn a, e, i, o, u — chìa khoá để bé đọc được từ thật
✓ Ghép âm thành từ: c-a-t → cat, đọc to, từng bước một
✓ 25 họ vần (-am, -an, -at, -ig, -op, -ug…)
✓ Tô nguyên cả từ — bé viết "cat", không chỉ viết "c"
✓ 8 truyện mới, chỉ dùng những từ bé đã đọc được

🎮 6 MINI-GAME mỗi unit
Bong Bóng Vỡ • Lật Thẻ Memory • Điền Chữ • Chọn Từ • Ghép Chữ • Kéo Thả Từ

📚 PHƯƠNG PHÁP PHONICS
Synthetic phonics — bé học mối liên hệ âm-chữ (phoneme-grapheme), ghép âm thành từ (blending), và đọc (decoding) tiếng Anh. Phương pháp dùng tại trường UK National Curriculum và US Common Core.

👨‍👩‍👧 AN TOÀN CHO BÉ
✓ Tuân thủ COPPA 100% — không thu thập thông tin cá nhân
✓ Không quảng cáo — không banner, không rewarded ad
✓ Không cần đăng ký, không email, không tài khoản
✓ Học offline sau khi tải level về
```

## Đổi đúng những gì so với live

**Thêm — 1 tiêu đề Level 1 đặt lên trên chính những dòng đã có sẵn, để khối Level 2 có
anh em chứ không mọc ra giữa trời. Chỉ gạch đầu dòng thứ 2 là chữ mới, còn lại là chữ live
dời xuống một mục:**

```
📖 LEVEL 1: BẢNG CHỮ CÁI
✓ Trọn 26 chữ A-Z với âm chuẩn bản xứ
✓ Dạy âm của chữ, không chỉ tên chữ — nền tảng để bé tự đọc
✓ Tô chữ thông minh — chấm điểm độ chính xác nét vẽ
✓ 8 truyện xây từ chính những chữ bé vừa học
```

**Thêm — khối mang thứ duy nhất Level 2 thực sự cho người mua:**

```
📖 LEVEL 2: NGUYÊN ÂM NGẮN
✓ Nguyên âm ngắn a, e, i, o, u — chìa khoá để bé đọc được từ thật
✓ Ghép âm thành từ: c-a-t → cat, đọc to, từng bước một
✓ 25 họ vần (-am, -an, -at, -ig, -op, -ug…)
✓ Tô nguyên cả từ — bé viết "cat", không chỉ viết "c"
✓ 8 truyện mới, chỉ dùng những từ bé đã đọc được
```

5 dòng, xếp theo sức bán: **ghép âm** đứng đầu vì đó là khoảnh khắc bé thôi nhìn mặt chữ mà
bắt đầu thật sự đọc. Họ vần và tô cả từ là hai thứ không đối thủ nào chụp lên screenshot.
Không gắn 🆕 — description là copy vĩnh viễn chứ không phải changelog, ship Level 3 là nó
thành sai. Muốn báo bản mới thì dùng ô "What's new" trong Play Console.

**Sửa — số truyện, cộng 2 chỗ chỉnh chữ:**

| Chỗ | Live | Bản mới | Vì sao |
|---|---|---|---|
| Câu mở | `8 truyện` | `16 truyện` | L2 thêm 8 truyện |
| Đoạn intro | `đọc 8 câu chuyện` | `đọc 16 câu chuyện` | như trên |
| Đoạn intro | `ghép âm thành từ` | `ghép âm thành từ thật` | thêm 1 chữ, cho intro mang keyword của L2 |
| BÉ HỌC GÌ | `8 câu chuyện — chữ sáng…` | `16 câu chuyện — chữ sáng…` | L2 thêm 8 truyện |
| BÉ HỌC GÌ | 4 gạch đầu dòng | 2 gạch — chỉ số liệu chung | 2 dòng kia là đặc thù Level 1, dời xuống tiêu đề `LEVEL 1`, giữ nguyên chữ |
| BÉ HỌC GÌ | `Bảng chữ cái A-Z với âm chuẩn bản xứ` | `Trọn 26 chữ A-Z với âm chuẩn bản xứ` | giờ nằm dưới `LEVEL 1`; `26` là con số cụ thể, khớp `curriculum.json` |
| AN TOÀN CHO BÉ | `Học hoàn toàn offline — không cần wifi` | `Học offline sau khi tải level về` | audio nằm trên Firebase, phải tải theo từng level (`feature/download`); chữ `hoàn toàn` sai ngay lần mở app đầu, và câu ngắn giữ được nhịp gạch đầu dòng |

**`488+ từ` giữ nguyên** — theo quyết định của chủ app, sau khi đã nêu rủi ro bên dưới.

**Giữ nguyên từng chữ** — toàn bộ dòng còn lại, gồm trọn mục `🎮`, `📚`, `👨‍👩‍👧` và câu mở
`"không quảng cáo"`.

## Rủi ro đã biết, chủ app chấp nhận

`488+ từ` là tổng từ vựng của cả 5 level, nhưng mới ship L1 và L2 — `LevelRepository.kt:31`
hiển thị L3–L5 là Coming Soon và chưa có asset audio/ảnh, nên 312 từ trong đó không ai chạm
tới được. Thật sự chơi được: **176 từ unique** (104 ở L1 + 94 ở L2, trùng 22). Store giờ đã
hiện badge "Mua hàng trong ứng dụng", nên con số nội dung bị thổi phồng nằm ngay cạnh một
sản phẩm trả phí.

Quyết định: giữ `488+`. Xem lại nếu Play tuýt còi hoặc review than thiếu nội dung. Nếu buộc
phải đổi thì `176 từ` là con số bảo vệ được, và 2 dòng cần sửa là câu mở với bullet
`✓ 488+ từ vựng có audio phát âm`.

## Dòng còn lại biết là thiếu nhưng giữ theo minimal-diff

- Không dòng nào nói mua trong ứng dụng là mua cái gì (2 unit đầu mỗi level free, sau đó mua
  đứt từng level). User đụng paywall mà không được báo trước. Không thêm, theo minimal-diff;
  xem lại nếu có review 1 sao than về paywall.

## Target keywords (priority order)

| Tier | Keyword | Target rank |
|---|---|---|
| 1 | `học tiếng anh cho bé` | Top 10 |
| 1 | `học đọc tiếng anh` | Top 20 |
| 1 | `tiếng anh trẻ em` | Top 20 |
| 2 | `phonics tiếng anh` | Top 10 |
| 2 | `ghép vần tiếng anh` | Top 10 ⭐ mới (Level 2) |
| 3 | `phonics cho bé` | Top 5 ⭐ |
| 3 | `app phonics trẻ em` | Top 5 ⭐ |
| 3 | `nguyên âm ngắn tiếng anh` | Top 5 ⭐ mới (Level 2) |

## Verified features (audit 2026-08-17)

| Claim | Source of truth |
|---|---|
| 488+ từ | `curriculum.json` — 488 word entries của **cả 5 level**; chỉ 176 từ vào được (xem "Rủi ro đã biết") |
| 16 truyện | `files/stories/level_1.json` (8) + `level_2.json` (8), đều có audio + word timing |
| 25 họ vần (L2) | `curriculum.json` — 30 `displayLetter` của L2 trừ 5 nguyên âm trần |
| 6 mini-game mỗi unit | `game/GameRegistry.kt` → `DEFAULT_UNIT_GAMES` |
| Karaoke word-sync | `step/common/KaraokeText.kt` |
| Tô chữ + chấm điểm | `step/tracing/TracingScorer.kt` (ngưỡng 75%) |
| Tô cả từ (L2+) | `step/wordtracing/` |
| Ghép âm (L2+) | `step/vowelblend/`, route ở `StepScreen.kt:59-61` |
| Không quảng cáo | `gradle/libs.versions.toml` không còn AdMob/AppLovin; store không có badge "Có quảng cáo" |
| L3–L5 vào không được | `LevelRepository.kt:31` → `LAUNCHED_PREMIUM_LEVELS = setOf("L2")`; chưa có asset audio/ảnh |

## Tuyệt đối KHÔNG claim

- ❌ "5 level" — mới vào được L1 và L2.
- ❌ "100% miễn phí" / "không có mua trong ứng dụng" — IAP đã live, store có badge.
- ❌ Nói trống "học offline" — phải tải audio từng level trước.
- ⚠️ "488+ từ" giữ theo quyết định của chủ app, không phải vì bảo vệ được — xem "Rủi ro đã biết".

## Action items

- [x] Đã gỡ ads khỏi code (AdMob + AppLovin)
- [x] Data Safety: Contains ads = No (đã verify trên live — không có badge)
- [x] Pricing: Free + in-app purchases (đã verify trên live — có badge)
- [ ] Paste bản này vào Play Console → vi-VN
- [ ] Chạy lại `fetch-live.py` sau khi publish để re-baseline `live.md`
- [ ] Chụp lại screenshot — bộ hiện tại chỉ có Level 1, chưa có khung ghép âm / tô cả từ
- [ ] Kiểm tra feature graphic có in cứng "488+ từ" hoặc "8 truyện" không
