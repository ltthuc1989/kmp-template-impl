# Play Store Listing — Vietnam (vi-VN) — PRIMARY MARKET

⚠️ **Mốc 1 strategy**: 100% FREE, NO ADS, NO Premium tier. Description không mention monetization.

## App title (24 chars)

```
Phonics Kids: Học Tiếng Anh
```

## Short description (76 chars)

```
Phonics tiếng Anh cho bé — 488+ từ, 8 truyện, 6 mini-game vui cho bé 3-8 ✨
```

## Long description (~830 chars)

```
Phonics Kids — học đọc tiếng Anh qua phonics. 488+ từ vựng, 8 truyện, 6 mini-game cho bé 3-8 tuổi.

Phonics Kids giúp bé 3-8 tuổi học đọc tiếng Anh qua phương pháp phonics. Bé học bảng chữ cái A-Z, ghép âm thành từ, đọc 8 câu chuyện — và chơi 6 mini-game vui sau mỗi bài.

🌟 BÉ HỌC GÌ
✓ Bảng chữ cái A-Z với âm chuẩn bản xứ
✓ 488+ từ vựng có audio phát âm
✓ 8 câu chuyện — chữ sáng theo lời đọc (karaoke style)
✓ Tô chữ thông minh — chấm điểm độ chính xác

🎮 6 MINI-GAME mỗi unit
Bong Bóng Vỡ • Lật Thẻ Memory • Điền Chữ • Chọn Từ • Ghép Chữ • Kéo Thả Từ

📚 PHƯƠNG PHÁP PHONICS
Synthetic phonics — phương pháp dùng tại trường UK National Curriculum và US Common Core.

👨‍👩‍👧 AN TOÀN CHO BÉ
✓ Tuân thủ COPPA 100% — không thu thập thông tin cá nhân
✓ Không cần đăng ký, không email, không tài khoản
✓ Hoàn toàn không quảng cáo

📧 support@abcphonicskids.com
🔒 https://ltthuc1989.github.io/phonics-kids/
```

## Target keywords (priority order)

| Tier | Keyword | Target rank |
|---|---|---|
| 1 | `học tiếng anh cho bé` | Top 10 |
| 1 | `học đọc tiếng anh` | Top 20 |
| 1 | `tiếng anh trẻ em` | Top 20 |
| 2 | `phonics tiếng anh` | Top 10 |
| 3 | `phonics cho bé` | Top 5 ⭐ |
| 3 | `app phonics trẻ em` | Top 5 ⭐ |

## Verified features (audit 2026-05-17)

| Feature | Code path |
|---|---|
| 488+ words | `curriculum.json` actual count |
| 8 stories | `L1_S01-L1_S08` with audio + word-timing |
| 6 mini-games | `game/{bubblepop,memorymatch,filletter,pickword,spellletters,dragwords}/` |
| Karaoke word-sync | `step/common/KaraokeText.kt` |
| Smart tracing | `step/tracing/TracingScorer.kt` (75% threshold) |
| COPPA-compliant | No personal data collection |

## Action items trước submit Play Console

- [ ] **Disable ads in code** (AdMob + AppLovin) — required cho "Hoàn toàn không quảng cáo" claim
- [ ] **Data Safety form**: "Contains ads" = **No**
- [ ] **Pricing model**: "Free" (no IAP cho Mốc 1)
- [ ] **No Premium tier** trong app cho đến Mốc 2
