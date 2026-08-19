# 04 — RevenueCat + Play Billing: setup từ đầu

Quy trình dựng in-app purchase từ số 0, viết để **dùng lại cho project sau**.

- **Phần A–F** là quy trình chung, không phụ thuộc app nào
- **Phần G–I** là phần riêng của ABC Phonics Kids

Muốn dùng cho app khác: thay [bảng §0](#0-giá-trị-riêng-của-project-này), giữ nguyên phần còn lại.

**Last verified**: 2026-08-18 — RevenueCat KMP `2.8.0+17.46.0`, đối chiếu trực tiếp với sources jar
và với file `.aab` release đang phát hành.

> ⚠️ Tên menu trên Play Console và RevenueCat đổi khá thường xuyên. Doc này mô tả **cái gì phải đúng**,
> còn đường đi trong UI có thể lệch chút so với lúc bạn đọc.

---

## 0. Giá trị riêng của project này

Thay bảng này khi copy sang app khác.

| Hạng mục | Giá trị |
|---|---|
| Package name | `com.beely.phonicskids` |
| Mô hình giá | Mua đứt từng level, **one-time non-consumable** |
| Product ID | `phonics_level_1` … `phonics_level_5`, `phonics_all_levels` |
| Entitlement ID | `level_1` … `level_5` |
| Curriculum level ID | `L1` … `L5` |
| Enum trong code | `core/billing/.../model/SubscriptionPlan.kt` |
| API key đọc từ | `local.properties` → `PURCHASE_ANDROID_API_KEY` (qua BuildKonfig) |

### Checklist ship một level mới

Ví dụ Level 3.

**Billing** — 4 bước ở 4 màn hình khác nhau, chi tiết ở [Phần B](#phần-b--tạo-một-sản-phẩm-lặp-lại-cho-mỗi-level):

1. Play Console → In-app products → `phonics_level_3` → **Activate**
2. RevenueCat → Products → import `phonics_level_3` → **Non-consumable**
3. RevenueCat → Entitlements → `level_3` → attach `phonics_level_3`
4. RevenueCat → Offerings → offering đang **Current** → + New Package → attach `phonics_level_3`

**Code** — đúng một dòng, nhưng quên là hỏng sạch:

```kotlin
// core/repository/.../LevelRepository.kt
private val LAUNCHED_PREMIUM_LEVELS = setOf("L2", "L3")   // ← thêm "L3"
```

L3–L5 đều `isPremium = true` trong curriculum.json. Thiếu dòng này thì level hiện **"Coming soon"**
vĩnh viễn — dù content đã publish, product đã Active, entitlement đã attach, package đã vào offering.
Người dùng không bao giờ vào tới paywall để mà mua.

**Không cần làm gì thêm:**

- ❌ String mới — paywall gọi tên level qua `level_name` = `"Level %1$d: %2$s"`, ghép từ curriculum.json
- ❌ Enum mới — `SubscriptionPlan.LEVEL_3` … `LEVEL_5` đã khai sẵn
- ❌ Destination mới — điều hướng theo `levelId`, không có màn riêng cho từng level

---

# PHẦN A — Chuẩn bị, làm một lần cho mỗi app

## A1. Play Console phải có bản build đã upload

Google Play **không cho tạo in-app product** khi app chưa từng có bản nào upload lên một track
(internal testing là đủ), và bản đó phải chứa permission `com.android.vending.BILLING`.

Permission này do Play Billing Library tự merge vào từ AAR — không cần khai tay. Kiểm tra bản đã build:

```bash
unzip -q app-release.aab -d /tmp/aab
python3 -c "
import re; d=open('/tmp/aab/base/manifest/AndroidManifest.xml','rb').read()
print([x.decode('utf8','ignore') for x in re.findall(rb'[ -~]{6,}', d) if 'illing' in x.decode('utf8','ignore')])"
```

Phải thấy `com.android.vending.BILLING` và `ProxyBillingActivity`.

## A2. Nối Play Console với RevenueCat bằng Service Account

RevenueCat cần quyền đọc Play để lấy danh sách product và xác thực giao dịch. Thiếu bước này thì
**offerings luôn rỗng** dù Play Console đã Active hết.

1. **Google Cloud Console** → chọn project đang link với Play Console → IAM & Admin →
   **Service Accounts** → Create service account
2. Tạo **JSON key** cho service account đó → tải file về (giữ kín, không commit)
3. **Play Console** → Users and permissions → Invite new user → dán email của service account
4. Cấp quyền cho nó — tối thiểu: *View app information*, *View financial data*,
   *Manage orders and subscriptions*
5. **RevenueCat** → Project settings → Apps → Google Play → upload file JSON vừa tải

> Quyền mới cấp thường mất tới ~36 giờ để Google propagate. RevenueCat sẽ báo lỗi credentials trong
> lúc chờ — không phải bạn làm sai.

## A3. Tạo Project và App trên RevenueCat

1. RevenueCat → **+ New Project**
2. Trong project → **Apps** → + New → **Google Play Store**
3. Nhập **package name** (phải khớp `applicationId` của bản release, không phải của bản debug)
4. Copy **Public SDK key** — dạng `goog_xxxxxxxx`

> Dùng **Public SDK key** (`goog_…`) cho app. Secret key (`sk_…`) chỉ dùng ở server, lộ ra là mất
> quyền kiểm soát tài khoản.

## A4. Đưa key vào app mà không commit

Project này đọc key từ `local.properties` (git-ignored) rồi nhúng qua BuildKonfig:

```properties
# local.properties
PURCHASE_ANDROID_API_KEY=goog_xxxxxxxxxxxxxxxxxxxx
PURCHASE_IOS_API_KEY=appl_xxxxxxxxxxxxxxxxxxxx
```

```kotlin
// composeApp/build.gradle.kts
setField("PURCHASE_ANDROID_API_KEY")   // đọc local.properties, fallback sang env var
```

Kiểm tra key đã thực sự vào bản release:

```bash
strings -a /tmp/aab/base/dex/*.dex | grep -oE "goog_[A-Za-z0-9]{8,}" | sort -u
```

> SDK RevenueCat có sẵn key mẫu `goog_1a2b3c4d5e6f7h` nằm trong dex — placeholder của SDK, vô hại.
> Key thật là cái còn lại.

⚠️ Nếu key rỗng, `configure()` return sớm và **không set `isConfigured`** → mọi lời gọi sau đó trả
rỗng, im lặng.

---

# PHẦN B — Tạo một sản phẩm, lặp lại cho mỗi level

Bốn bước, ở **bốn màn hình khác nhau**. Làm xong bước 2 rất dễ tưởng là xong cả — đó là lỗi phổ biến
nhất.

## B1. Play Console → tạo product

**Monetize → Products → In-app products → Create product**

| Field | Giá trị |
|---|---|
| Product ID | `phonics_level_2` — **không sửa được sau khi tạo** |
| Name / Description | hiển thị cho user |
| Price | theo từng quốc gia |

Bấm **Activate** — mới tạo mặc định là Inactive.

⚠️ **Phải vào mục In-app products, KHÔNG phải Subscriptions.** Lý do ở [§C1](#c1-storeproductid--bẫy-lớn-nhất).

## B2. RevenueCat → import product

**Products → + New → Google Play** → nhập `phonics_level_2`

RevenueCat sẽ hỏi **consumable hay non-consumable**:

> ### Chọn Non-consumable
>
> (hoặc trả lời **No** cho câu *"Is this product consumable?"*)
>
> | | Non-consumable ✅ | Consumable ❌ |
> |---|---|---|
> | Token giao dịch | Acknowledge, **giữ lại** | **Consume** → Play cho mua lại |
> | Vào `entitlements.active` | vĩnh viễn | không bao giờ |
> | `restorePurchases()` lấy lại | được | mất khi cài lại máy |
> | Dùng cho | mua đứt, remove ads | xu, gem, lượt chơi |
>
> **Chọn nhầm consumable thì hỏng thế này:** Play trừ tiền → `PurchaseResult.Success` → nhưng
> RevenueCat consume token và không cấp entitlement → `entitlements.active` rỗng →
> `SubscriptionState.Free` → `isPremium()` false → app báo **PurchaseFailed**.
>
> Kết quả: **đã trừ tiền, app báo mua thất bại, level vẫn khoá.** Và vì đã consume nên Play cho mua
> lại → user trả tiền nhiều lần cho cùng một thứ.
>
> **Lỡ chọn nhầm**: sửa lại type trong RevenueCat → Products. Giao dịch test cũ có thể còn kẹt trạng
> thái — huỷ ở Play Console → Order management rồi đợi vài phút, hoặc đổi tài khoản test khác.

## B3. RevenueCat → Entitlement + attach

**Entitlements → + New** → identifier `level_2` → mở ra → **Associated Products** → chọn
`phonics_level_2` → Save.

### Entitlement là gì, "attach" là gì

**Product** = thứ bán ra (có giá, có ID ở store). **Entitlement** = quyền mà việc mua đó cấp cho user.
*Attach* là nối hai cái lại: *"ai mua `phonics_level_2` thì được cấp quyền `level_2`"*.

Ví dụ dễ hình dung: vé là product, quyền vào cửa là entitlement. Nhiều loại vé khác nhau có thể cùng
cho một quyền vào cửa.

| Product | Attach vào entitlement |
|---|---|
| `phonics_level_1` … `phonics_level_5` | `level_1` … `level_5` (một-một) |
| `phonics_all_levels` (bundle) | **cả 5**: `level_1` … `level_5` |

Bundle chỉ là một product attach vào 5 entitlement. Nhờ vậy code không cần một dòng `if` nào cho
bundle — `entitlementId`/`levelId` của `SubscriptionPlan.BUNDLE` để null là có chủ đích.

**Code đọc entitlement, không đọc product.** App không hỏi "đã mua `phonics_level_2` chưa", nó hỏi
"quyền `level_2` có đang active không". Product ID chỉ dùng lúc hiện giá và lúc gọi mua.

→ Sau này đổi giá phải tạo product ID mới thì chỉ cần attach product mới vào cùng entitlement:
**không sửa code, không ra bản app mới**.

⚠️ Identifier phải khớp tuyệt đối `SubscriptionPlan.LEVEL_2.entitlementId` = `"level_2"`. Gõ
`Level_2` / `level2` / `L2` là hỏng im lặng: `LEVEL_ENTITLEMENTS[it]` trả null, `mapNotNull` bỏ qua,
không lỗi nào được ném ra.

## B4. RevenueCat → Offering + Package

**Chỉ có MỘT offering được đọc.** Code chỉ nhìn offering đang đặt **Current**:

```kotlin
private suspend fun currentPackages(): List<Package>? {
    val offerings = getOfferings() ?: return null
    return offerings.current?.availablePackages   // ← chỉ CURRENT
}
```

- ✅ **Đúng**: một offering Current chứa nhiều package — `phonics_level_1`, `phonics_level_2`, …
- ❌ **Sai**: mỗi level một offering riêng. Đặt "Level 2 offering" làm Current sẽ **làm chết Level 1**.

Cách làm: **Offerings** → mở offering đang có nhãn **Current** → **+ New Package** → attach
`phonics_level_2` → Save.

Tên package (`identifier`) và `packageType` code **không dùng tới** — đặt sao cũng được. Gợi ý dùng
`level_2` cho dễ đọc, type **Lifetime** hoặc **Custom**.

---

# PHẦN C — Hai cái bẫy làm hỏng im lặng

## C1. `storeProduct.id` — bẫy lớn nhất

Code khớp sản phẩm theo `storeProduct.id`, **không phải** theo tên package:

```kotlin
private fun List<Package>.findByPlan(plan: SubscriptionPlan): Package? = firstOrNull {
    it.storeProduct.id == plan.androidProductId || it.storeProduct.id == plan.iosProductId
}
```

Định dạng `id` theo doc của SDK (`StoreProduct.kt`):

```
Google INAPP: "<productId>"             → phonics_level_2        ✅ khớp
Google Sub:   "<productId:basePlanID>"  → phonics_level_2:xxx    ❌ KHÔNG khớp
```

Tạo nhầm thành subscription → `id` có thêm hậu tố base plan → `findByPlan` không bao giờ khớp →
danh sách product rỗng → paywall không có giá. Không lỗi nào được ném ra.

## C2. Bốn nguyên nhân làm danh sách product rỗng

`getProducts()` trả rỗng ở bốn tình huống, tất cả đều chỉ log rồi nuốt:

| Nguyên nhân | Dấu hiệu |
|---|---|
| API key rỗng | `configure()` return sớm, `isConfigured` vẫn false |
| `getOfferings()` lỗi mạng | log `Failed to fetch offerings` |
| Chưa đặt offering nào là Current | `offerings.current == null` |
| Product không nằm trong Current offering | **không log gì** — `mapNotNull` lặng lẽ bỏ qua |

Cộng thêm hai nguyên nhân phía dashboard: thiếu Service Account credentials ([A2](#a2-nối-play-console-với-revenuecat-bằng-service-account)),
và độ trễ Play Console (product vừa Activate thường mất vài phút đến vài giờ mới mua được).

---

# PHẦN D — Test

## D1. Build type nào dùng billing thật

`USE_FAKE_BILLING = isDebugBuild`, đọc `BuildConfig.DEBUG` của **module `core:billing`**, không phải
của app.

| Lệnh build | Billing dùng | Ghi chú |
|---|---|---|
| `assembleDebug` | 🟡 Fake | mua luôn thành công, không có dialog Play |
| `assembleBilling` | 🟡 Fake | ⚠️ sai ý đồ — xem [§I](#phần-i--vấn-đề-đã-biết-chưa-sửa) |
| `assembleRelease` | 🟢 RevenueCat thật | **cách duy nhất** test billing thật |

## D2. Tài khoản test

- **Play Console → Setup → License testing** → thêm email tài khoản test
- App phải cài **qua Play track** (internal testing). Sideload APK thì Play Billing không chạy
- Tester mua không mất tiền thật; huỷ giao dịch ở **Play Console → Order management** để test lại

## D3. Đọc log

Napier **không init ở release** (`GrabeeApplication.kt`: `if (BuildConfig.DEBUG) Napier.base(...)`),
nên mọi `Napier.e(...)` trong `RevenueCatBillingDataSource` im lặng hoàn toàn trên bản phát hành.

Nhưng SDK RevenueCat tự log ở mức INFO trong release (doc `Purchases.logLevel`: *"By default,
LogLevel.DEBUG in debug builds, and LogLevel.INFO in release builds"*), độc lập với Napier:

```bash
adb logcat -c && adb logcat | grep -iE "Purchases|BillingClient"
```

Cần chi tiết hơn: đặt `Purchases.logLevel = LogLevel.VERBOSE` **trước** `Purchases.configure(...)`.

## D4. Chẩn đoán theo triệu chứng

| Triệu chứng | Nguyên nhân gần như chắc chắn |
|---|---|
| Paywall **không có giá** | Product chưa là Package trong offering **Current** ([B4](#b4-revenuecat--offering--package)) |
| Mua được, **trừ tiền, nhưng không mở khoá** | Chưa attach entitlement ([B3](#b3-revenuecat--entitlement--attach)), hoặc lỡ chọn consumable ([B2](#b2-revenuecat--import-product)) |
| Mua xong **mua lại được nữa** | Đã chọn consumable |
| Tất cả level đều hỏng | Chưa đặt offering nào Current / thiếu Service Account / key sai |
| Chỉ **một** level hỏng | So sánh nó với level đang chạy được, 4 dòng ở [§E](#phần-e--checklist-nghiệm-thu) |

---

# PHẦN E — Checklist nghiệm thu

Mở song song level đang chạy được và level mới, đối chiếu 4 dòng:

| Kiểm tra ở đâu | Phải thấy gì |
|---|---|
| Play Console → In-app products | product = **Active** |
| RevenueCat → Products | type = **Non-consumable** |
| RevenueCat → Entitlements | `level_n` có attach đúng product |
| RevenueCat → Offerings → cái đang **Current** | có Package chứa product đó |

Rồi chạy `adb logcat -c && adb logcat | grep -i Purchases`, mở paywall → phải hiện **đúng 1 card giá**.

---

# PHẦN F — iOS (làm sau)

Cùng entitlement, khác store:

1. **App Store Connect** → In-App Purchases → **Non-Consumable** (chỗ này có chọn thật, khác Play)
2. Product ID: dùng **cùng chuỗi** với Android — enum đã thiết kế vậy
   (`androidProductId == iosProductId`)
3. RevenueCat → Apps → + New → App Store → nhập bundle ID → lấy key `appl_…`
4. Import product → attach vào **cùng entitlement `level_n`** đã có
5. Package trong **cùng offering Current** — RevenueCat tự chọn product theo store của thiết bị

Điền `PURCHASE_IOS_API_KEY` vào `local.properties` (hiện đang trống).

---

# PHẦN G — Hợp đồng giữa code và dashboard

Bốn định danh phải khớp, nằm ở bốn nơi:

| Định danh | Ví dụ (Level 2) | Khai ở đâu |
|---|---|---|
| Play product ID | `phonics_level_2` | Play Console + RevenueCat Product |
| Entitlement ID | `level_2` | RevenueCat Entitlements |
| Curriculum level ID | `L2` | `core/resource/.../files/curriculum.json` |
| Enum | `SubscriptionPlan.LEVEL_2` | `core/billing/.../model/SubscriptionPlan.kt` |

Enum là nơi buộc cả bốn lại:

```kotlin
LEVEL_2("phonics_level_2", "phonics_level_2", "level_2", "L2")
//       androidProductId   iosProductId      entitlementId  levelId
```

Hai luồng dùng chúng khác nhau:

- **Hiện giá** — `getProducts()` khớp `storeProduct.id` == `androidProductId`
- **Quyền sở hữu** — `updateFrom()` đọc `info.entitlements.active.keys` rồi map qua
  `SubscriptionPlan.LEVEL_ENTITLEMENTS` (`level_n` → `Ln`)

```kotlin
val owned = info.entitlements.active.keys
    .mapNotNull { SubscriptionPlan.LEVEL_ENTITLEMENTS[it] }
    .toSet()
_subscriptionState.value =
    if (owned.isEmpty()) SubscriptionState.Free else SubscriptionState.Premium(owned)
```

`isPremium` chỉ là `this is Premium`, mà `Premium` chỉ sinh ra khi `owned.isNotEmpty()` — nên **không
có entitlement là chắc chắn rơi vào nhánh fail**, không có đường nào khác.

Thêm level mới vào code: chỉ cần một dòng trong enum. `LEVEL_ENTITLEMENTS`, `allLevelIds`,
`forLevel()` đều sinh ra từ đó.

---

# PHẦN H — Soi trực tiếp file release

Khi nghi bản build thiếu thứ gì:

```bash
unzip -q composeApp/release/composeApp-release.aab -d /tmp/aab

# Play Billing có trong dex không
strings -a /tmp/aab/base/dex/*.dex | grep -c "com/android/billingclient"

# API key RevenueCat có được nhúng không
strings -a /tmp/aab/base/dex/*.dex | grep -oE "goog_[A-Za-z0-9]{8,}" | sort -u

# Permission + activity render dialog mua
python3 -c "
import re; d=open('/tmp/aab/base/manifest/AndroidManifest.xml','rb').read()
print([x.decode('utf8','ignore') for x in re.findall(rb'[ -~]{6,}', d) if 'illing' in x.decode('utf8','ignore')])"
```

Bản `0.0.5` (2026-08-18) đạt hết: permission `com.android.vending.BILLING`,
`ProxyBillingActivity` + `ProxyBillingActivityV2`, 25 ref tới Play Billing, API key thật đã nhúng.

---

# PHẦN I — Vấn đề đã biết, chưa sửa

| Vấn đề | Chi tiết | Ảnh hưởng |
|---|---|---|
| Biến thể `billing` dùng fake billing | `isDebuggable = true` → `BuildConfig.DEBUG = true`; `core:billing` chỉ có variant debug/release nên `matchingFallbacks.add("debug")` kéo về debug | Không test được billing thật bằng `assembleBilling`. Fix: cho `USE_FAKE_BILLING` đọc từ `AppConfig` thay vì `BuildConfig.DEBUG` |
| Napier không init ở release | `GrabeeApplication.kt` | Mọi log billing biến mất trên bản phát hành |
| `proguard-rules.pro` không tồn tại | `composeApp/build.gradle.kts` khai `proguardFiles(..., "proguard-rules.pro")` nhưng file không có trong repo | Build vẫn qua (AGP bỏ qua file thiếu), billing không hỏng vì RevenueCat ship consumer rules trong AAR. Nhưng R8 đang chạy chỉ với rule mặc định |

## Đã sửa

**2026-08-18** — hai đường nuốt lỗi trong `PaywallViewModel` khiến mua hỏng mà không có dialog lẫn
thông báo:

- `purchase()` mở đầu bằng `products.find { … } ?: return` → thoát im lặng, `_purchaseState` giữ
  `Idle`. Nay đặt `PurchaseFailed` để snackbar hiện.
- `fetch()` coi danh sách rỗng là thành công → paywall render với plan selector rỗng và nút Buy bấm
  không làm gì. Nay trả `ScreenState.Error(error_billing)` kèm retry.

---

## Liên quan

- [01-PRD.md](01-PRD.md) — mô hình giá per-level, unit free mỗi level
- [02-TECH_SPEC.md](02-TECH_SPEC.md) — module `core:billing`, `feature:billing`
- `core/model/.../MonetizationConfig.kt` — `MONETIZATION_ENABLED`, `FREE_UNITS_PER_LEVEL`
