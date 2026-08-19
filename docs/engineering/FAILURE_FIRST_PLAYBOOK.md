# Playbook — chống lỗi im lặng

Viết sau một buổi debug billing tìm ra 7 lỗi. Chúng **không phải 7 lỗi độc lập** — chung đúng một
gốc. Doc này rút gốc đó ra thành luật và quy trình, để project sau không phải trả giá lại.

Không phụ thuộc project nào. Ví dụ lấy từ ABC Phonics Kids nhưng luật áp dụng chung.

---

## 1. Gốc chung: mọi đường hỏng đều im lặng

Bảy lỗi tìm được, và điều người dùng thực sự trải nghiệm:

| # | Lỗi | Người dùng thấy | Dev thấy |
|---|---|---|---|
| 1 | `purchase()` bắt đầu bằng `products.find { } ?: return` | Bấm nút, **không có gì xảy ra** | không log |
| 2 | `fetch()` coi danh sách rỗng là thành công | Paywall trống, nút bấm vô hiệu | không log |
| 3 | `isPremium()` (toàn cục) dùng cho paywall per-level | Restore → văng về màn trước, level vẫn khoá | không log |
| 4 | Napier chỉ init khi `BuildConfig.DEBUG` | — | **mất sạch log ở bản release** |
| 5 | Biến thể `billing` thực chất dùng fake billing | Test "thành công" giả | không cảnh báo |
| 6 | `proguard-rules.pro` khai trong build nhưng không tồn tại | — | AGP bỏ qua, không báo |
| 7 | Compose Resources copy file cũ không xoá | APK không giảm dù đã strip | build ra byte-identical, không warning |

Nhìn cột "Dev thấy": **7/7 không có tín hiệu nào**. Đó mới là bug thật sự. Bảy cái ở cột đầu chỉ là
triệu chứng.

> **Gốc: code được viết cho đường hạnh phúc. Mọi đường còn lại trả về `null` / `emptyList` /
> `false` / `return` — tức là trả về "không có gì", thay vì trả về "đây là chuyện đã xảy ra".**

"Không có gì" là trạng thái hợp lệ trong mọi ngôn ngữ, nên compiler không cản, test không bắt, code
review đọc lướt thấy hợp lý. Nó chỉ lộ ra khi có người dùng thật ngồi trước màn hình câm.

---

## 2. Bảy luật, mỗi luật rút từ một lỗi thật

### R1 — Mọi hành động người dùng thấy được phải đổi trạng thái quan sát được

Không có đường nào thoát khỏi một handler mà không để lại dấu vết.

```kotlin
// ❌ Bấm nút → không gì xảy ra
fun purchase() {
    val product = products.find { it.plan == selected } ?: return
}

// ✅ Mọi nhánh đều dẫn tới một state
fun purchase() {
    val product = products.find { it.plan == selected }
    if (product == null) { _state.value = PurchaseFailed; return }
}
```

**Cách soát**: tìm mọi `?: return`, `?: return@`, `if (x == null) return` nằm trong hàm được gọi từ
`onClick`. Mỗi cái phải trả lời được: "người dùng biết gì sau dòng này?"

### R2 — Rỗng từ nguồn xa là lỗi, không phải thành công

Danh sách rỗng trả về từ mạng/store/DB gần như luôn có nghĩa "hỏng ở đâu đó", không phải "đúng là
không có gì".

```kotlin
// ❌ Rỗng lọt vào nhánh thành công
.fold(onSuccess = { ScreenState.Idle(it) }, ...)

// ✅ Rỗng có nhánh riêng, kèm retry
onSuccess = { if (it.products.isEmpty()) ScreenState.Error(...) else ScreenState.Idle(it) }
```

Ngoại lệ hợp lệ (danh sách bookmark rỗng thật) thì phải có **empty state riêng**, khác màn lỗi —
chứ không phải để nó rơi vào màn thành công trống trơn.

### R3 — Boolean trả lời câu hỏi có phạm vi thì phải nhận phạm vi làm tham số

Đây là lỗi nguy hiểm nhất, vì code đọc lên **nghe rất đúng**.

```kotlin
if (billingRepository.isPremium()) { closePaywall() }   // ❌ đọc xuôi tai, sai
```

`isPremium()` trả lời về **người dùng**. Câu hỏi thật là về **người dùng × level**. Tên hàm đúng ngữ
pháp nên qua được cả code review lẫn compiler.

```kotlin
fun ownsPaywallTarget(levelId: String?, owned: Set<String>, isPremium: Boolean): Boolean
```

**Cách soát**: mỗi boolean dùng để ra quyết định, hỏi *"đúng với ai, với cái gì, lúc nào?"*. Thiếu
chiều nào thì chiều đó phải thành tham số.

### R4 — Log phải to nhất ở bản bạn ship

```kotlin
if (BuildConfig.DEBUG) { Napier.base(DebugAntilog()) }   // ❌ tắt log đúng lúc cần nhất
```

Bản debug là bản bạn ngồi cạnh, cắm cáp, đặt breakpoint được. Bản release là bản chạy trên máy người
lạ, cách bạn nửa vòng trái đất. Tắt log ở đó là tự bịt mắt đúng lúc mù nhất.

Ship log ở release: mức INFO/ERROR, không PII, gắn crash reporter. Debug thì VERBOSE.

### R5 — Biến thể "để test" phải chạm hàng thật, không thì xoá đi

Biến thể `billing` sinh ra để test billing thật với release signing. Nhưng `isDebuggable = true` →
`BuildConfig.DEBUG = true` → dùng fake billing. **Nó test chính cái fake.**

Một môi trường test không chạm dependency thật thì tệ hơn là không có: nó phát tín hiệu xanh giả.

**Cách soát**: mỗi biến thể/flavor "để test X", hỏi *"chạy nó lên, X thật có được gọi không?"*.
Chứng minh bằng log hoặc traffic, không bằng suy luận.

### R6 — Cấu hình ngoài phải khớp code thì ghim bằng test

Billing yêu cầu 4 chuỗi khớp nhau ở 4 nơi khác nhau (Play Console, RevenueCat products, RevenueCat
entitlements, curriculum.json). Sai một ký tự → hỏng im lặng.

Không test được dashboard, nhưng **test được phía code**:

```kotlin
@Test fun everyPlanPinsItsStoreIds() {
    assertEquals("phonics_level_2", SubscriptionPlan.LEVEL_2.androidProductId)
    assertEquals("level_2",         SubscriptionPlan.LEVEL_2.entitlementId)
}
```

Test này không chứng minh dashboard đúng. Nó thu hẹp vùng nghi ngờ xuống còn một nửa — và khi đi
hỏi người khác, bạn nói được "phía code đã ghim, vấn đề ở dashboard".

### R7 — Luật quyết định nằm ở hàm thuần, top-level

Nếu luật nằm trong ViewModel/Repository cần Room + DataStore mới dựng được, nó sẽ **không bao giờ
được test**.

```kotlin
// Test được không cần fake nào
internal fun purchaseOutcome(result: PurchaseResult, ownsTarget: Boolean): PurchaseUiState
```

ViewModel còn lại phần keo: gọi I/O, gọi hàm thuần, gán state. Phần keo có thể bỏ test; phần luật
thì không.

---

## 3. Thực hành cốt lõi: **bảng thất bại viết TRƯỚC khi code**

Đây là thứ đáng giá nhất trong doc này. Sáu trong bảy lỗi trên sẽ bị chặn ngay ở bàn giấy nếu có
bảng này.

Trước khi viết dòng code đầu tiên của một tính năng, điền bảng:

| Hỏng ở đâu | Người dùng thấy gì | Tôi thấy gì | Test nào bắt |
|---|---|---|---|
| Chưa có sản phẩm nào trả về | Màn lỗi + nút Thử lại | `log.e("empty offerings")` | `fetch_emptyList_showsError` |
| Mua xong nhưng quyền không về | "Mua thất bại", ở lại paywall | `log.e("granted=false")` | `purchase_successNoEntitlement_fails` |
| Bấm restore khi chưa mua gì | Snackbar "không có gì để khôi phục" | INFO | `restore_nothing_staysOnScreen` |
| Mất mạng giữa chừng | Màn lỗi + Thử lại | `log.e(exception)` | `fetch_networkError_showsError` |

**Luật đọc bảng:**

> Bất kỳ dòng nào cột "Người dùng thấy gì" ghi *"không thấy gì"* hoặc bạn không điền nổi — **đó là
> bug thiết kế, sửa trước khi code.**

> Bất kỳ dòng nào cột "Tôi thấy gì" để trống — **bạn đang tự nguyện mù ở production.**

Bảng này mất 15 phút. Buổi debug sinh ra doc này mất nhiều giờ, cộng một lần suýt để lọt bug trừ
tiền mà không mở khoá ra tay người dùng thật.

---

## 4. Với tính năng phụ thuộc cấu hình bên ngoài

Bất cứ thứ gì cần dashboard/console/biến môi trường mới chạy (billing, push, analytics, CDN, feature
flag) — thêm hai thứ vào spec:

**(a) Bảng hợp đồng** — cái gì phải khớp cái gì, ở đâu:

| Định danh | Giá trị | Khai ở đâu | Ai là nguồn chân lý |
|---|---|---|---|
| Product ID | `phonics_level_2` | Play Console + RevenueCat + enum | Play Console |
| Entitlement | `level_2` | RevenueCat + enum | enum trong code |

**(b) Bảng chẩn đoán ngược** — triệu chứng → nguyên nhân. Viết lúc thiết kế, không phải lúc cháy:

| Triệu chứng | Nguyên nhân gần như chắc chắn |
|---|---|
| Paywall không có giá | Product chưa vào offering đang Current |
| Mua được nhưng không mở khoá | Chưa attach entitlement, hoặc chọn nhầm consumable |

Bảng (b) chính là thứ biến một buổi debug mò mẫm thành ba câu hỏi có thứ tự.

---

## 5. Cổng nghiệm thu — chưa qua thì chưa xong

Đừng nói "xong" khi chưa tick đủ:

- [ ] Mọi dòng trong bảng thất bại đều có màn hình/thông báo tương ứng, **đã tự tay kích hoạt thử**
- [ ] Mọi dòng đều có log, và log đó **hiện ra ở bản release**
- [ ] Luật quyết định nằm ở hàm thuần, có test
- [ ] Định danh khớp cấu hình ngoài đã ghim bằng test
- [ ] Đã chạy trên **đúng loại build sẽ ship** (không phải debug, không phải emulator nếu tính năng
      cần dịch vụ thật)
- [ ] Có người khác đọc được bảng chẩn đoán ngược và tự sửa được

Gạch đầu dòng áp chót đắt nhất. Emulator không làm được Play Billing; bản debug dùng fake. Test trên
chúng rồi kết luận "chạy tốt" là **kết luận về thứ khác**, không phải về sản phẩm bạn ship.

---

## 6. Thứ tự áp dụng cho project mới

1. **Trước khi code tính năng** — điền bảng thất bại (§3). Dòng nào "không thấy gì" thì sửa thiết kế.
2. **Nếu có cấu hình ngoài** — thêm bảng hợp đồng + bảng chẩn đoán ngược (§4).
3. **Khi code** — luật vào hàm thuần (R7), rỗng-là-lỗi (R2), boolean nhận phạm vi (R3).
4. **Trước khi ship** — bật log ở release (R4), ghim định danh bằng test (R6), chạy đúng loại build.
5. **Khi có bug** — sửa xong thì thêm dòng vào bảng chẩn đoán ngược. Bảng dày lên theo thời gian là
   tài sản của project.

---

## 7. Một câu tóm tắt

> Phần mềm không hỏng vì lập trình viên viết sai đường hạnh phúc. Nó hỏng vì **không ai viết đường
> còn lại** — và trong máy tính, "không viết gì" luôn là code chạy được.
