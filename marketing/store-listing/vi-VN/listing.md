# Play Store Listing — Vietnam (vi-VN) — PRIMARY MARKET

⚠️ **Bản nháp Mốc 4 — thêm Level 4 và Level 5.** So với [live.md](live.md) trước khi paste
vào Play Console. Refresh snapshot trước: `python3 marketing/store-listing/fetch-live.py vi-VN`.

**Live đang là bản Mốc 3 (L1–L3)** — `live.md` fetch 2026-10-04, store "Cập nhật" 24 thg 9
2026; long description khớp từng chữ với bản nháp Mốc 3. Short description lên live đã bỏ
`, không quảng cáo`, bản này giữ như live. Lần này chỉ đổi: thêm khối `LEVEL 4` và
`LEVEL 5`, thêm dòng `5 level`, và `24 truyện` → `40 truyện` ở short description, câu mở
và mục `BÉ HỌC GÌ`. Phần còn lại giữ nguyên.

Khối L4, L5 theo đúng luật của L2/L3 bên dưới: âm nào cũng đi kèm một từ chứa nó, từ nào
cũng đã soát có thật trong đúng level đó của `curriculum.json`.

**Mốc 4 vòng 2 (chủ app, 2026-10-04): L4, L5 chỉ nói bé đọc được gì, bỏ phần giảng luật.**
Bản nháp đầu của L4/L5 giảng luật chính tả (`fr trong frog`, `sh trong fish`,
`c trong cat và city`, đuôi từ) — chủ app thấy khó hiểu. Giờ L4/L5 chỉ liệt kê từ ví dụ, cộng
dòng chữ câm in mờ. **L1–L3 và mọi mục khác giữ nguyên** — giống hệt bản live.

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

## Short description (55 chars) — ĐỔI (24 → 40 truyện)

```
Học đọc tiếng Anh cho bé 3-8 — bắt đầu từ âm, 40 truyện
```

Bỏ chữ `phonics` ở đây vì tên app đã mang sẵn, mà Play index tên app chung với short
description. `bắt đầu từ âm` nói đúng thứ app làm, bằng chữ phụ huynh vẫn dùng. Câu này phủ
được cả 2 keyword Tier 1 (`học tiếng anh cho bé`, `học đọc tiếng anh`).

## Long description

```
Phonics Kids — bé 3-8 tuổi học đọc tiếng Anh, từng âm một. Không quảng cáo. 5 level, 488+ từ, 40 truyện, 6 mini-game.

Đọc tiếng Anh bắt đầu từ âm, không phải từ mặt chữ. Phonics Kids dạy bé âm của từng chữ cái, rồi dạy cách ghép các âm đó lại thành từ — đúng cách trường học ở Anh và Mỹ đang dạy. Đích đến là bé tự đọc được một từ chưa ai đọc cho nghe bao giờ.

🌟 BÉ HỌC GÌ
✓ 5 level — từ A đến Z cho tới những từ dài như television
✓ 488+ từ, từ nào cũng có tiếng đọc
✓ 40 truyện — chữ sáng lên theo lời người kể

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

📖 LEVEL 4: PHỤ ÂM GHÉP
✓ Bé đọc được những từ khó hơn: frog, snake, fish, three, splash
✓ 96 từ mới trong 24 bài học
✓ 8 truyện mới

📖 LEVEL 5: CHỮ GHÉP VÀ TỪ DÀI
✓ Bé đọc được từ dài: banana, umbrella, television
✓ Âm câm như k trong knife, w trong write — app in mờ để bé dễ nhận ra
✓ 96 từ mới trong 24 bài học
✓ 8 truyện mới

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

`488+ từ` là tổng word **entry** của cả 5 level. Từ Mốc 4 cả 5 level đều đã ship
(`LevelRepository.kt:39` → `LAUNCHED_PREMIUM_LEVELS = setOf("L2", "L3", "L4", "L5")`), nên
không còn từ nào nằm sau "Coming Soon". Khoảng hở duy nhất còn lại là từ lặp: 488 entry =
**426 từ unique** (62 từ xuất hiện ở hơn một level).

Quyết định: giữ `488+` (chủ app, Mốc 3). Rủi ro giờ nhỏ hơn nhiều so với lúc chấp nhận (khi
đó mới 264/488 vào được). Nếu buộc phải đổi thì `420+ từ` là con số bảo vệ được tuyệt đối;
sửa câu mở và bullet `✓ 488+ từ, từ nào cũng có tiếng đọc`.

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
| 3 | `phụ âm ghép` | Tiêu đề LEVEL 4 |
| 3 | `chữ câm` | LEVEL 5 bullet 2 |
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

## Verified features (audit 2026-10-04)

Nguồn chi tiết từng dòng xem bảng cùng tên trong [en-US/listing.md](../en-US/listing.md) —
hai locale dùng chung một bộ claim và cùng một bộ từ ví dụ.

| Claim trong bài | Nguồn |
|---|---|
| 5 level | `curriculum.json` L1–L5; `LevelRepository.kt:39` mở L2–L5 |
| 488+ từ | 488 entry cả 5 level (426 unique), đều vào được |
| 40 truyện | `stories/level_1.json` … `level_5.json`, mỗi level 8 truyện, 32/32 scene có `word_timings` |
| từ ví dụ L4 (frog, snake, stop, fish, lunch, three, string, splash, city, giraffe) | có thật trong L4 `curriculum.json`; cat, goat ở L1 |
| từ ví dụ L5 (car, bird, nurse, house, boy, draw, knife, write, lamb, tiger, banana, umbrella, television, picture, station, beautiful) | có thật trong L5 `curriculum.json` |
| chữ câm in nhạt (L5) | `SILENT_ALPHA = 0.4f` trong `step/vowelblend/ClusterBlendContent.kt` (bước ghép âm L5 U7) |
| 96 từ, 24 bài (L3, L4, L5) | 8 unit × 3 bài, 96 từ unique mỗi level |
| L4, L5 mua được | `SubscriptionPlan.kt:27-28` → `phonics_level_4`, `phonics_level_5` |
| không quảng cáo | `gradle/libs.versions.toml` không còn AdMob/AppLovin; store không có badge |

**Số liệu không còn dùng trong bài** (giữ lại phòng khi cần): 25 họ vần ở L2; 18 kiểu viết
nguyên âm dài ở L3 (4 split digraph + 14 cặp nguyên âm).

## Tuyệt đối KHÔNG claim

- ❌ "100% miễn phí" / "không có mua trong ứng dụng" — IAP đã live, store có badge.
- ❌ Nói trống "học offline" — phải tải audio từng level trước.
- ❌ "từ nào cũng có tranh" — 82/96 từ L3 chỉ có emoji, chỉ 14 từ có ảnh WebP.
- ❌ "chữ câm nào cũng in nhạt" ở mọi màn — chỉ có ở bước ghép âm Level 5.
- ⚠️ "488+ từ" giữ theo quyết định của chủ app; unique là 426.

## Action items

- [x] Đã gỡ ads khỏi code (AdMob + AppLovin)
- [x] Data Safety: Contains ads = No (đã verify trên live)
- [x] Pricing: Free + in-app purchases (đã verify trên live)
- [x] Bản Mốc 3 (L2 + L3 + viết lại) đã lên live — verify 2026-10-04
- [ ] Paste bản này vào Play Console → vi-VN (thêm L4 + L5, 40 truyện)
- [ ] Chạy lại `fetch-live.py` sau khi publish để re-baseline `live.md`
- [ ] Chụp lại screenshot — thêm ít nhất 1 màn Level 4/5
- [ ] Kiểm tra feature graphic có in cứng "24 truyện" / "8 truyện" không
- [ ] `phonics_level_4`, `phonics_level_5` phải live + có giá trên Play Console
- [x] Chốt tiêu đề riêng cho vi-VN: `ABC Phonics - Tiếng Anh Trẻ Em` (2026-09-02)
- [ ] Đặt tên này ở Play Console → vi-VN → App name (KHÔNG đổi ở locale khác)
- [ ] Sau khi đổi tên, theo dõi thứ hạng `tiếng anh trẻ em` và `tiếng anh cho bé` ~2 tuần —
      đây là thay đổi ASO lớn nhất từ trước tới nay, cần biết nó ăn hay không
