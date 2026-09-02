# Play Store Listing — Vietnam (vi-VN) — PRIMARY MARKET

⚠️ **Mốc 3 draft.** So với [live.md](live.md) trước khi paste vào Play Console.
Refresh snapshot trước: `python3 marketing/store-listing/fetch-live.py vi-VN`.

**Live vẫn là bản thời Level 1.** Bản nháp Mốc 2 (khối Level 2) viết xong nhưng chưa bao giờ
paste — `live.md` (fetch 2026-08-17, store "Cập nhật" 25 thg 6 2026) vẫn ghi `8 truyện` và
không có khối level nào. Nên bản này là **một lần paste thêm CẢ Level 2 lẫn Level 3**, không
phải cộng dồn lên thứ đã publish.

**Nguyên tắc gần-minimal-diff.** Không viết lại dòng nào cho vui. Chỉ đổi: 3 khối level song
song, các con số ship L2 + L3 làm sai, và 1 câu offline nói sai sự thật. `BÉ HỌC GÌ` thu lại
còn số liệu chung để các khối level song song nhau — có `LEVEL 2` mà không có `LEVEL 1` phía
trên thì đọc như thể thiếu mất Level 1. Các gạch đầu dòng của nó chuyển xuống `LEVEL 1`,
giữ nguyên chữ.

## App title (16 chars) — GIỮ NGUYÊN

```
ABC Phonics Kids
```

## Short description (75 chars) — ĐỔI

```
Phonics tiếng Anh cho bé 3-8 — A-Z, nguyên âm ngắn & dài, 24 truyện, 6 game
```

## Long description — THÊM 2 khối, sửa số truyện, nói rõ dòng offline

```
Phonics Kids — học đọc tiếng Anh cho bé 3-8 tuổi qua phonics, không quảng cáo. 488+ từ vựng, 24 truyện, 6 mini-game.

Phonics Kids giúp bé 3-8 tuổi học đọc tiếng Anh qua phương pháp phonics. Bé học bảng chữ cái A-Z, ghép âm thành từ thật, đọc 24 câu chuyện — và chơi 6 mini-game vui sau mỗi bài.

🌟 BÉ HỌC GÌ
✓ 488+ từ vựng có audio phát âm
✓ 24 câu chuyện — chữ sáng theo lời đọc (karaoke style)

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

📖 LEVEL 3: NGUYÊN ÂM DÀI
✓ Magic e — thêm một chữ e câm, cap thành cape, kit thành kite
✓ Cặp nguyên âm: ai, ay, ee, ea, igh, oa, ow, oo và 6 cặp nữa
✓ 18 kiểu viết nguyên âm dài, trải 24 bài học
✓ Từ giữ nguyên khối — app phóng to đúng phần đang được đánh vần
✓ 8 truyện mới, câu dài hơn, vẫn toàn từ bé tự đọc được

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

**Thêm — 1 tiêu đề Level 1 đặt lên trên chính những dòng đã có sẵn, để các khối level mới có
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

**Thêm — Level 3:**

```
📖 LEVEL 3: NGUYÊN ÂM DÀI
✓ Magic e — thêm một chữ e câm, cap thành cape, kit thành kite
✓ Cặp nguyên âm: ai, ay, ee, ea, igh, oa, ow, oo và 6 cặp nữa
✓ 18 kiểu viết nguyên âm dài, trải 24 bài học
✓ Từ giữ nguyên khối — app phóng to đúng phần đang được đánh vần
✓ 8 truyện mới, câu dài hơn, vẫn toàn từ bé tự đọc được
```

Magic e đứng đầu vì đó là luật phonics duy nhất phụ huynh **nhìn một dòng là hiểu** —
`cap → cape`, khỏi giải thích, khỏi cần screenshot. Cặp nguyên âm đứng thứ hai: đó là danh
sách cụ thể, và `ai / ee / oa / igh` chính là chuỗi phụ huynh gõ vào ô tìm kiếm.
`18 kiểu viết, 24 bài` là bằng chứng khối lượng. Dòng 4 là điểm khác duy nhất so với Level 2
— L2 cắt từ thành thẻ rồi cộng lại (`t` + `an`), L3 giữ nguyên khối và phóng to phần đang
đọc, vì nguyên âm dài chỉ tồn tại dưới dạng mẫu (`a_e` bị phụ âm chen giữa; cắt ra là hỏng).

Không gắn 🆕 ở đâu cả — description là copy vĩnh viễn chứ không phải changelog, ship Level 4
là nó thành sai. Muốn báo bản mới thì dùng ô "What's new" trong Play Console.

**Sửa — số truyện, cộng 2 chỗ chỉnh chữ:**

| Chỗ | Live | Bản mới | Vì sao |
|---|---|---|---|
| Câu mở | `8 truyện` | `24 truyện` | L2 + L3 mỗi cấp thêm 8 truyện |
| Đoạn intro | `đọc 8 câu chuyện` | `đọc 24 câu chuyện` | như trên |
| Đoạn intro | `ghép âm thành từ` | `ghép âm thành từ thật` | thêm 1 chữ, cho intro mang keyword của L2/L3 |
| BÉ HỌC GÌ | `8 câu chuyện — chữ sáng…` | `24 câu chuyện — chữ sáng…` | L2 + L3 mỗi cấp thêm 8 truyện |
| BÉ HỌC GÌ | 4 gạch đầu dòng | 2 gạch — chỉ số liệu chung | 2 dòng kia là đặc thù Level 1, dời xuống tiêu đề `LEVEL 1`, giữ nguyên chữ |
| BÉ HỌC GÌ | `Bảng chữ cái A-Z với âm chuẩn bản xứ` | `Trọn 26 chữ A-Z với âm chuẩn bản xứ` | giờ nằm dưới `LEVEL 1`; `26` là con số cụ thể, khớp `curriculum.json` |
| AN TOÀN CHO BÉ | `Học hoàn toàn offline — không cần wifi` | `Học offline sau khi tải level về` | audio nằm trên Firebase, phải tải theo từng level (`feature/download`); chữ `hoàn toàn` sai ngay lần mở app đầu, và câu ngắn giữ được nhịp gạch đầu dòng |

**Short description** — `nguyên âm ngắn` là keyword của Mốc 2; giờ nó chung dòng với
`nguyên âm dài`. Viết gộp `nguyên âm ngắn & dài`: tốn thêm 6 ký tự so với dòng Mốc 2, và
tiết kiệm 9 ký tự so với viết đủ hai vế. Số truyện chạy theo phần thân. 75/80 ký tự.

**`488+ từ` giữ nguyên** — theo quyết định của chủ app, sau khi đã nêu rủi ro bên dưới.

**Giữ nguyên từng chữ** — toàn bộ dòng còn lại, gồm trọn mục `🎮`, `📚`, `👨‍👩‍👧` và câu mở
`"không quảng cáo"`. Tiêu đề level khớp tên trong app (`curriculum.json` → `"The Alphabet"`,
`"Short Vowels"`, `"Long Vowels"`), nên store và app nói cùng một thứ.

## Rủi ro đã biết, chủ app chấp nhận

`488+ từ` là tổng word entry của cả 5 level, nhưng mới ship L1–L3 — `LevelRepository.kt:31`
(`LAUNCHED_PREMIUM_LEVELS = setOf("L2", "L3")`) hiển thị L4–L5 là Coming Soon và chưa có
asset audio/ảnh. Thật sự chơi được: **264 từ unique** (104 ở L1 + 94 ở L2 + 96 ở L3, trùng
30). Store có badge "Mua hàng trong ứng dụng", nên con số nội dung bị thổi phồng nằm ngay
cạnh một sản phẩm trả phí.

Level 3 kéo khoảng cách này lại đáng kể — Mốc 2 mới có 176/488 chạm tới được, giờ là 264/488.

Quyết định: giữ `488+`. Xem lại nếu Play tuýt còi hoặc review than thiếu nội dung. Nếu buộc
phải đổi thì `264 từ` là con số bảo vệ được, và 2 dòng cần sửa là câu mở với bullet
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
| 2 | `ghép vần tiếng anh` | Top 10 (Level 2) |
| 3 | `phonics cho bé` | Top 5 ⭐ |
| 3 | `app phonics trẻ em` | Top 5 ⭐ |
| 3 | `nguyên âm ngắn tiếng anh` | Top 5 (Level 2) |
| 3 | `nguyên âm dài tiếng anh` | Top 5 ⭐ mới (Level 3) |
| 3 | `magic e` / `e câm` | Top 5 ⭐ mới (Level 3) |

## Verified features (audit 2026-08-31)

| Claim | Source of truth |
|---|---|
| 488+ từ | `curriculum.json` — 488 word entry của **cả 5 level** (426 unique); chỉ 264 từ vào được (xem "Rủi ro đã biết") |
| 24 truyện | `files/stories/level_1.json` (8) + `level_2.json` (8) + `level_3.json` (8), đều có audio + word timing |
| 25 họ vần (L2) | `curriculum.json` — 30 `displayLetter` của L2 trừ 5 nguyên âm trần |
| 18 kiểu viết nguyên âm dài (L3) | `displayLetter` của L3 — 4 split digraph (`a_e i_e o_e u_e`) + 14 cặp nguyên âm (`ai ay ee ea y ey igh ie oa ow ue ui ew oo`); 8 họ vần (`ame ake ate ave ime ike ive ine`) dạy lồng bên trong, KHÔNG đếm |
| 24 bài, 8 unit (L3) | `curriculum.json` — L3 `"Long Vowels"`, 8 unit × 3 bài, 96 từ |
| 26 chữ, 8 unit (L1) | `curriculum.json` — L1 `"The Alphabet"`, 26 bài, chữ A–Z |
| 6 mini-game mỗi unit | `game/GameRegistry.kt` → `DEFAULT_UNIT_GAMES` |
| Karaoke word-sync | `step/common/KaraokeText.kt`; L3 có `word_timings` đủ 32/32 scene |
| Tô chữ + chấm điểm | `step/tracing/TracingScorer.kt` (ngưỡng 75%) |
| Tô cả từ (L2 + L3) | `step/wordtracing/`, route ở `StepScreen.kt:267` cho mọi level trừ L1 |
| Ghép âm (L2) | `step/vowelblend/VowelBlendContent` — cắt từ thành thẻ rồi cộng lại |
| Ghép vần theo mẫu (L3) | `step/vowelblend/PatternBlendContent.kt` — từ giữ nguyên khối, phóng to phần đang đọc |
| Audio L3 đủ | `files/audio/level_3/` — 8 unit × 27 file + 32 file truyện = 248 |
| L3 mua được | `SubscriptionPlan.kt:26` → `LEVEL_3("phonics_level_3", …)` |
| L4–L5 vào không được | `LevelRepository.kt:31` → `LAUNCHED_PREMIUM_LEVELS = setOf("L2", "L3")`; chưa có asset audio/ảnh |
| Không quảng cáo | `gradle/libs.versions.toml` không còn AdMob/AppLovin; store không có badge "Có quảng cáo" |

## Tuyệt đối KHÔNG claim

- ❌ "5 level" — mới vào được L1, L2 và L3.
- ❌ "100% miễn phí" / "không có mua trong ứng dụng" — IAP đã live, store có badge.
- ❌ Nói trống "học offline" — phải tải audio từng level trước.
- ⚠️ "488+ từ" giữ theo quyết định của chủ app, không phải vì bảo vệ được — xem "Rủi ro đã biết".

## Action items

- [x] Đã gỡ ads khỏi code (AdMob + AppLovin)
- [x] Data Safety: Contains ads = No (đã verify trên live — không có badge)
- [x] Pricing: Free + in-app purchases (đã verify trên live — có badge)
- [ ] Paste bản này vào Play Console → vi-VN (thêm **cả** L2 lẫn L3 so với live)
- [ ] Chạy lại `fetch-live.py` sau khi publish để re-baseline `live.md`
- [ ] Chụp lại screenshot — bộ hiện tại chỉ có Level 1, chưa có khung ghép âm / tô cả từ / magic e
- [ ] Kiểm tra feature graphic có in cứng "488+ từ" hoặc "8 truyện" không
- [ ] `phonics_level_3` phải live + có giá trên Play Console trước khi đẩy copy này
