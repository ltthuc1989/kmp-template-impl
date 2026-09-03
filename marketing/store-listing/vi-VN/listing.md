# Play Store Listing — Vietnam (vi-VN) — PRIMARY MARKET

⚠️ **Mốc 3 draft.** So với [live.md](live.md) trước khi paste vào Play Console.
Refresh snapshot trước: `python3 marketing/store-listing/fetch-live.py vi-VN`.

**Live vẫn là bản thời Level 1.** Bản nháp Mốc 2 (khối Level 2) viết xong nhưng chưa bao giờ
paste — `live.md` (fetch 2026-08-17, store "Cập nhật" 25 thg 6 2026) vẫn ghi `8 truyện` và
không có khối level nào. Bản này là **một lần paste, thêm cả Level 2 lẫn Level 3 và viết lại
toàn bộ chữ cho dễ hiểu.**

**Bỏ nguyên tắc minimal-diff.** Mốc 2 cố giữ chữ của bản live. Giữ như thế là giữ luôn một
lỗi thật: cả bài **gọi tên âm chứ không chỉ vào âm**, nên phụ huynh chưa đọc được tiếng Anh
thành tiếng thì không hiểu. Sửa cái đó không phải sửa vài dòng, nên viết lại hết. Mọi con số
vẫn soát lại từ code — xem "Verified features".

## Luật của bản viết lại này

**Không ai viết được tiếng ra giấy. Nhưng ai cũng chỉ được vào một từ có chứa tiếng đó.**

Neo mỗi âm vào một từ quen (`âm a trong cat`) vừa là cách dạy chuẩn của mọi giáo trình
phonics, vừa là cách duy nhất để một dòng chữ trong store truyền được âm thanh tới người
chưa phát âm được ví dụ. Ba hệ quả:

1. **Không gọi tên âm nào mà không kèm từ.** Không viết "nguyên âm ngắn a" — viết "âm a
   trong cat".
2. **Bỏ mọi ví dụ cần nghe mới hiểu.** `cap → cape` là vô hình với người không đọc được cả
   hai từ. Cắt hẳn; để chính những từ bé đọc được làm nhiệm vụ thuyết phục.
3. **Mỗi dòng phải tự đứng được.** Người ta lướt store chứ không đọc từ trên xuống. Dòng nào
   phải nhớ dòng khác mới hiểu là dòng hỏng.

Cắt luôn: `phoneme-grapheme`, `decoding`, `synthetic phonics`, `họ vần`, `split digraph`.
Đúng hết, nhưng đó là từ vựng đào tạo giáo viên. Riêng **synthetic** người thường đọc thành
"tổng hợp / nhân tạo" — ngược hẳn nghĩa thật (ghép các âm lại thành từ), tức là nó đang
phản chủ.

## App title (30 chars) — ĐỔI (riêng cho vi-VN)

```
ABC Phonics - Tiếng Anh Trẻ Em
```

Play cho đặt tên hiển thị riêng theo từng locale, và đây là trường có trọng số cao nhất
trong tìm kiếm. Bản cũ `ABC Phonics Kids` không mang một chữ tiếng Việt nào, tức là bỏ trắng
trường mạnh nhất ở đúng thị trường chính. Giữ `ABC Phonics` phía trước để ai tìm đúng tên
vẫn ra, phần sau gánh keyword Tier 1 `tiếng anh trẻ em`.

Tên này chỉ đổi trên store, **không đụng tới `app_name`** — tên hiện dưới icon trên máy vẫn
là `ABC Phonics Kids`, không cần build lại. Các locale khác cũng giữ nguyên tên cũ.

⚠️ **Vừa đúng 30/30 ký tự, không còn dư chỗ nào.** Thêm bất cứ thứ gì cũng bị Play từ chối;
muốn sửa thì phải bớt chỗ khác trước.

## Short description (72 chars) — ĐỔI

```
Học đọc tiếng Anh cho bé 3-8 — bắt đầu từ âm, 24 truyện, không quảng cáo
```

Bỏ chữ `phonics` ở đây vì tên app đã mang sẵn, mà Play index tên app chung với short
description. `bắt đầu từ âm` nói đúng thứ app làm, bằng chữ phụ huynh vẫn dùng. Câu này phủ
được cả 2 keyword Tier 1 (`học tiếng anh cho bé`, `học đọc tiếng anh`).

## Long description

```
Phonics Kids — bé 3-8 tuổi học đọc tiếng Anh, từng âm một. Không quảng cáo. 488+ từ, 24 truyện, 6 mini-game.

Đọc tiếng Anh bắt đầu từ âm, không phải từ mặt chữ. Phonics Kids dạy bé âm của từng chữ cái, rồi dạy cách ghép các âm đó lại thành từ — đúng cách trường học ở Anh và Mỹ đang dạy. Đích đến là bé tự đọc được một từ chưa ai đọc cho nghe bao giờ.

🌟 BÉ HỌC GÌ
✓ 488+ từ, từ nào cũng có tiếng đọc
✓ 24 truyện — chữ sáng lên theo lời người kể

📖 LEVEL 1: BẢNG CHỮ CÁI
✓ Trọn 26 chữ, từ A đến Z
✓ Không chỉ thuộc bài hát ABC — bé học mỗi chữ đọc ra tiếng gì
✓ Bé tô từng chữ, app chấm từng nét
✓ 8 truyện ghép từ chính những chữ bé vừa học

📖 LEVEL 2: NGUYÊN ÂM NGẮN
✓ Âm a trong cat, e trong bed, i trong big, o trong hot, u trong cup
✓ Bé đọc to từng âm rồi ghép lại: c - a - t, cat
✓ Đổi một chữ, đọc được cả nhóm: cat, hat, bat, mat
✓ Bé tô cả từ — viết "cat", không chỉ viết "c"
✓ 8 truyện mới, từ nào cũng là từ bé đã học

📖 LEVEL 3: NGUYÊN ÂM DÀI
✓ Bé đọc được từ dài hơn: cake, home, happy, blue, moon
✓ Cùng một âm mà viết nhiều kiểu — rain hay day, bé đều đọc đúng
✓ 96 từ mới trong 24 bài học
✓ 8 truyện mới — bé tự đọc, bố mẹ không phải đọc hộ

🎮 6 MINI-GAME mỗi unit
Bong Bóng Vỡ • Lật Thẻ Memory • Điền Chữ • Chọn Từ • Ghép Chữ • Kéo Thả Từ

📚 DẠY THEO CÁCH NÀO
Giống hệt cách bé đánh vần tiếng Việt — "bờ - a - ba" — chỉ khác là ghép vần bằng âm tiếng Anh. Bé học từng chữ đọc ra tiếng gì, rồi ghép vần lại thành từ. Không học thuộc mặt chữ; từ nào bé cũng tự ghép ra được. Đây là cách dạy phonics tiếng Anh của chương trình Anh (UK National Curriculum) và Mỹ (US Common Core).

👨‍👩‍👧 AN TOÀN CHO TRẺ EM
✓ Không quảng cáo — không banner, không video, không bao giờ
✓ Không cần đăng ký, không email, không tài khoản
✓ Không thu thập thông tin cá nhân (COPPA)
✓ Học offline sau khi tải level về
```

## Vì sao từng mục đổi

| Mục | Trước | Giờ | Lý do |
|---|---|---|---|
| Câu mở | `học đọc tiếng Anh cho bé 3-8 tuổi qua phonics, không quảng cáo` | `bé 3-8 tuổi học đọc tiếng Anh, từng âm một` | Play cắt sau ~80 ký tự; dòng đầu phải tự bán được. `từng âm một` nói ra phương pháp mà không cần gọi tên nó |
| Intro | `qua phương pháp phonics` + `ghép âm thành từ thật` | `bắt đầu từ âm, không phải từ mặt chữ` + `ghép các âm đó lại thành từ` | `phương pháp phonics` là nhắc lại tên app; `bắt đầu từ âm chứ không phải mặt chữ` mới là điều phụ huynh cần biết, và đối lập thẳng với app dạy nhớ mặt chữ |
| Cuối intro | *(không có)* | `tự đọc được một từ chưa ai đọc cho nghe bao giờ` | Đây là định nghĩa tiếng Việt thuần của **decoding** — đúng thứ phụ huynh bỏ tiền mua. Nói được mà không cần dùng từ đó |
| BÉ HỌC GÌ | `488+ từ vựng có audio phát âm` | `488+ từ, từ nào cũng có tiếng đọc` | `từ vựng`, `audio`, `phát âm` đều thay được bằng chữ phụ huynh vẫn nói |
| LEVEL 1 | `Dạy âm của chữ, không chỉ tên chữ — nền tảng để bé tự đọc` | `Không chỉ thuộc bài hát ABC — bé học mỗi chữ đọc ra tiếng gì` | Phân biệt âm-với-tên-chữ là phân biệt quan trọng nhất của phonics giai đoạn đầu, nhưng phụ huynh **không cần nắm khái niệm đó**. Bài hát ABC là thứ ai cũng nhận ra ngay là "thuộc chữ rồi mà vẫn chưa đọc được" — gọi tên đúng khoảng trống mà không phải dạy ai điều gì. Bản trước viết `"s" kêu sss, không phải "ét"`: `ét` là tên chữ S trong tiếng Việt còn câu thì đang nói tiếng Anh, bắt người đọc nhảy giữa hai hệ |
| LEVEL 2 | `Nguyên âm ngắn a, e, i, o, u — chìa khoá để bé đọc được từ thật` | `Âm a trong cat, e trong bed, i trong big, o trong hot, u trong cup` | `nguyên âm ngắn a` là tiếng lóng nhà nghề, phụ huynh không hình dung được. 5 mỏ neo, cả 5 từ đã kiểm tra là có thật trong L2 |
| LEVEL 2 | `25 họ vần (-am, -an, -at, -ig, -op, -ug…)` | `Đổi một chữ, đọc được cả nhóm: cat, hat, bat, mat` | `họ vần` là từ nhà nghề, mà dãy `-am -an -at` với phụ huynh chỉ là ký tự lạ. Bản mới **làm cho xem** thay vì gọi tên |
| LEVEL 3 | `Magic e — cap thành cape` + `Từ giữ nguyên khối, app phóng to phần đang đánh vần` | `Bé đọc được từ dài hơn: cake, home, happy, blue, moon` + `Cùng một âm mà viết nhiều kiểu — rain hay day` | Xem "Luật của bản viết lại". Dòng cũ thứ hai mô tả cơ chế màn hình theo góc nhìn của code, không phải lợi ích của người mua |
| PHƯƠNG PHÁP | `synthetic phonics`, `phoneme-grapheme`, `blending`, `decoding` | neo vào **đánh vần tiếng Việt**: `"bờ - a - ba" — chỉ khác là bằng âm tiếng Anh` | Đổi tên mục thành `DẠY THEO CÁCH NÀO`. Mọi phụ huynh Việt đều đã tự tay đánh vần, nên đây là thao tác họ đã biết trong xương tuỷ — khỏi phải giải thích ghép âm là gì. Câu sắc thứ hai là `Không học thuộc mặt chữ`: đó là điểm đối lập với các app dạy whole-word, tức là lợi thế cạnh tranh |
| AN TOÀN | `COPPA 100%` đứng đầu | `Không quảng cáo` đứng đầu, COPPA xuống cuối | Xếp lại theo thứ tự phụ huynh thật sự quyết định. `rewarded ad` → `video` |

**`488+ từ` giữ nguyên** — theo quyết định của chủ app, sau khi đã nêu rủi ro bên dưới.

**Giữ nguyên từng chữ** — danh sách game `🎮`, và `Học offline sau khi tải level về`.

## Rủi ro đã biết, chủ app chấp nhận

`488+ từ` là tổng word entry của cả 5 level, nhưng mới ship L1–L3 — `LevelRepository.kt:31`
(`LAUNCHED_PREMIUM_LEVELS = setOf("L2", "L3")`) hiển thị L4–L5 là Coming Soon và chưa có
asset audio/ảnh. Thật sự chơi được: **264 từ unique** (104 ở L1 + 94 ở L2 + 96 ở L3, trùng
30). Store có badge "Mua hàng trong ứng dụng", nên con số bị thổi phồng nằm ngay cạnh một
sản phẩm trả phí.

Level 3 kéo khoảng cách lại đáng kể — Mốc 2 mới 176/488, giờ 264/488.

Quyết định: giữ `488+`. Xem lại nếu Play tuýt còi hoặc review than thiếu nội dung. Nếu buộc
phải đổi thì `264 từ` là con số bảo vệ được, và 2 dòng cần sửa là câu mở với bullet
`✓ 488+ từ, từ nào cũng có tiếng đọc`.

## Dòng còn lại biết là thiếu nhưng vẫn giữ

- Không dòng nào nói mua trong ứng dụng là mua cái gì (2 unit đầu mỗi level free, sau đó mua
  đứt từng level). User đụng paywall mà không được báo trước. Xem lại nếu có review 1 sao
  than về paywall.

## Target keywords

| Tier | Keyword | Nằm ở đâu |
|---|---|---|
| 1 | `học tiếng anh cho bé` | Short description + câu mở |
| 1 | `học đọc tiếng anh` | Short description + câu mở |
| 1 | `tiếng anh trẻ em` | **Tên app** (trường nặng nhất) + tiêu đề mục `AN TOÀN CHO TRẺ EM` |
| 2 | `phonics tiếng anh` | Tên app + DẠY THEO CÁCH NÀO (`cách dạy phonics tiếng Anh`) |
| 2 | `ghép vần tiếng anh` | DẠY THEO CÁCH NÀO (`ghép vần` ×2) + LEVEL 2 |
| 3 | `phonics cho bé` | Tên app + body |
| 3 | `nguyên âm ngắn` / `nguyên âm dài` | Tiêu đề khối level |
| 3 | `dạy bé đọc tiếng anh` | Intro |

Bản viết lại đánh đổi một phần mật độ keyword khớp-chính-xác lấy khả năng hiểu, rồi lấy lại
những chỗ lấy được mà không mất chữ dễ hiểu nào: `ghép vần` (0 → 2, và vốn là chữ tự nhiên
hơn `ghép các tiếng lại`), `trẻ em` (0 → 1, đổi tiêu đề mục), `phonics tiếng Anh` (0 → 1).
Play xếp hạng một phần theo tỉ lệ chuyển đổi, mà chữ khó hiểu thì kéo tỉ lệ đó xuống — nên
dừng ở đây, không nhồi thêm.

**Đòn bẩy lớn nhất đã dùng: tiêu đề riêng cho vi-VN** — `ABC Phonics - Tiếng Anh Trẻ Em`,
đưa keyword Tier 1 vào trường nặng nhất. Chốt ngày 2026-09-02. Xem mục "App title".

Tên app và short description giờ chia nhau hai cách gọi: tiêu đề mang `trẻ em`, short
description mang `bé` (`Học đọc tiếng Anh cho bé 3-8`). Phủ được cả hai chữ mà không chỗ
nào phải nhắc lại chữ nào.

## Verified features (audit 2026-09-02)

| Claim trong bài | Nguồn |
|---|---|
| 488+ từ | `curriculum.json` — 488 word entry của **cả 5 level** (426 unique); chỉ 264 vào được (xem "Rủi ro đã biết") |
| 24 truyện | `stories/level_1.json` (8) + `level_2.json` (8) + `level_3.json` (8), đều có audio + word timing |
| chữ sáng theo lời kể | `step/common/KaraokeText.kt`; L3 đủ `word_timings` 32/32 scene |
| 26 chữ A–Z (L1) | `curriculum.json` — L1 `"The Alphabet"`, 26 bài |
| tô chữ + chấm nét (L1) | `step/tracing/TracingScorer.kt` (ngưỡng 75%) |
| cat / bed / big / hot / cup (L2) | cả 5 từ đã kiểm tra là có trong L2 `curriculum.json` |
| cat, hat, bat, mat (L2) | họ `-at` của L2 = `bat cat hat mat rat` |
| tô cả từ (L2, L3) | `step/wordtracing/`, route ở `StepScreen.kt:267` cho mọi level trừ L1 |
| cake, home, happy, blue, moon (L3) | cả 5 từ đã kiểm tra là có trong L3 `curriculum.json` |
| rain, day — cùng âm, 2 kiểu viết (L3) | L3 U4 `ai` (rain) và `ay` (day), cùng `soundSpelling: "aaay"` |
| 96 từ, 24 bài (L3) | `curriculum.json` — L3 `"Long Vowels"`, 8 unit × 3 bài |
| 6 mini-game mỗi unit | `game/GameRegistry.kt` → `DEFAULT_UNIT_GAMES` |
| audio L3 đủ | `files/audio/level_3/` — 8 unit × 27 file + 32 file truyện = 248 |
| L3 mua được | `SubscriptionPlan.kt:26` → `LEVEL_3("phonics_level_3", …)` |
| không quảng cáo | `gradle/libs.versions.toml` không còn AdMob/AppLovin; store không có badge |

**Số liệu không còn dùng trong bài** (giữ lại phòng khi cần): 25 họ vần ở L2; 18 kiểu viết
nguyên âm dài ở L3 (4 split digraph + 14 cặp nguyên âm).

## Tuyệt đối KHÔNG claim

- ❌ "5 level" — mới vào được L1, L2, L3.
- ❌ "100% miễn phí" / "không có mua trong ứng dụng" — IAP đã live, store có badge.
- ❌ Nói trống "học offline" — phải tải audio từng level trước.
- ❌ "từ nào cũng có tranh" — 82/96 từ L3 chỉ có emoji, chỉ 14 từ có ảnh WebP.
- ⚠️ "488+ từ" giữ theo quyết định của chủ app, không phải vì bảo vệ được.

## Action items

- [x] Đã gỡ ads khỏi code (AdMob + AppLovin)
- [x] Data Safety: Contains ads = No (đã verify trên live)
- [x] Pricing: Free + in-app purchases (đã verify trên live)
- [ ] Paste bản này vào Play Console → vi-VN (thêm L2 + L3 và bản viết lại)
- [ ] Chạy lại `fetch-live.py` sau khi publish để re-baseline `live.md`
- [ ] Chụp lại screenshot — bộ hiện tại chỉ có Level 1
- [ ] Kiểm tra feature graphic có in cứng "488+ từ" hoặc "8 truyện" không
- [ ] `phonics_level_3` phải live + có giá trên Play Console trước khi đẩy copy này
- [x] Chốt tiêu đề riêng cho vi-VN: `ABC Phonics - Tiếng Anh Trẻ Em` (2026-09-02)
- [ ] Đặt tên này ở Play Console → vi-VN → App name (KHÔNG đổi ở locale khác)
- [ ] Sau khi đổi tên, theo dõi thứ hạng `tiếng anh trẻ em` và `tiếng anh cho bé` ~2 tuần —
      đây là thay đổi ASO lớn nhất từ trước tới nay, cần biết nó ăn hay không
