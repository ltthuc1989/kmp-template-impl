# Workflow giao hàng một app — từ lên kế hoạch tới publish

Xương sống nối mọi tài liệu, script và kỹ năng của repo thành **một trình tự có cổng chặn**.

Doc này **không lặp lại** nội dung các doc khác. Nó trả lời: *làm gì, theo thứ tự nào, và điều kiện
gì phải đúng trước khi được đi tiếp*.

- Áp dụng cho project mới: đọc từ Pha 0, thay ví dụ bằng app của bạn.
- Áp dụng cho ABC Phonics Kids: nhảy tới pha đang làm, đọc phần Cổng của pha trước để soát ngược.

> **Nguyên tắc xuyên suốt**: mỗi pha có một **Cổng**. Chưa qua cổng thì không sang pha sau. Cổng
> không phải thủ tục — mỗi mục trong cổng sinh ra từ một lần đã trả giá.

---

## Bản đồ tài liệu

| Cần gì | Đọc đâu |
|---|---|
| Luật viết code chống lỗi im lặng | [FAILURE_FIRST_PLAYBOOK.md](FAILURE_FIRST_PLAYBOOK.md) |
| Sản phẩm: ai dùng, tính năng, nội dung | [../abc-phonics-kids/01-PRD.md](../abc-phonics-kids/01-PRD.md) |
| Kỹ thuật: module, schema, navigation | [../abc-phonics-kids/02-TECH_SPEC.md](../abc-phonics-kids/02-TECH_SPEC.md) |
| Timeline, mốc, risk register | [../abc-phonics-kids/03-IMPLEMENTATION_PLAN.md](../abc-phonics-kids/03-IMPLEMENTATION_PLAN.md) |
| Billing / IAP | [../abc-phonics-kids/04-BILLING_SETUP.md](../abc-phonics-kids/04-BILLING_SETUP.md) |
| Store listing, brand, asset | [../../marketing/README.md](../../marketing/README.md) |
| Pattern code cụ thể | `.claude/skills/grabee/references/` (14 file) |
| Bump version + build AAB | skill `/release` |
| Scaffold 1 unit phonics | `/scaffold-unit` |

---

# PHA 0 — Chốt phạm vi trước khi viết dòng code nào

Pha rẻ nhất và bị bỏ qua nhiều nhất. Mọi quyết định ở đây mà lật lại sau sẽ đắt gấp bội.

Phải chốt bằng văn bản:

1. **Ship cái gì ở bản đầu** — và quan trọng hơn: **không ship cái gì**. Viết mục "Out of Scope" rõ
   như mục tính năng.
2. **Thị trường mục tiêu, theo thứ tự** — quyết định này chi phối ngôn ngữ, giá, và cả cách phát âm
   trong audio.
3. **Mô hình kiếm tiền** — thuê bao / mua đứt / theo cấp độ / quảng cáo. Đổi mô hình sau khi code
   xong là viết lại cả tầng billing.
4. **Phạm vi i18n** — dịch màn nào, để nguyên màn nào.
   > Luật đã chốt ở project này: *phạm vi UI = phạm vi mô tả cửa hàng*. Dịch 9 locale mà chỉ đủ sức
   > nuôi 2 là tự tạo nợ.
5. **Đối tượng người dùng có làm phát sinh nghĩa vụ pháp lý không** — app trẻ em, y tế, tài chính
   đều kéo theo ràng buộc phải biết **từ Pha 0**, không phải lúc điền form ở Pha 6.

### 🚧 Cổng 0

- [ ] Có mục "Out of Scope" viết thành câu, không phải ngụ ý
- [ ] Thứ tự thị trường đã xếp hạng, không phải "toàn cầu"
- [ ] Mô hình giá đã chốt, có **một** nguồn chân lý duy nhất cho con số
- [ ] Đã biết mình rơi vào diện pháp lý đặc biệt nào chưa

> **Bẫy đã dẫm**: giá tồn tại 3 con số khác nhau trong repo (PRD ghi thuê bao $9.99/tháng, comment
> code ghi $5/level, fake billing ghi $6.99). Không cái nào là thật — giá thật nằm ở Play Console.
> Từ Pha 0 hãy ghi rõ: *"nguồn chân lý của giá là X, mọi nơi khác chỉ là placeholder"*.

---

# PHA 1 — Spec

Ba tài liệu, ba câu hỏi khác nhau. Đừng gộp.

| Doc | Trả lời | Người đọc |
|---|---|---|
| **PRD** | Làm cho ai, giải quyết gì, nội dung gì | PM, designer, chính bạn 3 tháng sau |
| **TECH SPEC** | Module nào, schema nào, màn nào, tiêu chí nghiệm thu từng REQ | Dev |
| **IMPLEMENTATION PLAN** | Tuần nào làm gì, mốc nào, rủi ro nào | Bạn, để không trôi |

**Bắt buộc trong TECH SPEC — thứ hay thiếu nhất:**

- **Tiêu chí nghiệm thu cho từng REQ.** "Xong" phải kiểm chứng được, không phải cảm nhận.
- **Bảng thất bại cho từng tính năng có I/O.** Xem [Playbook §3](FAILURE_FIRST_PLAYBOOK.md). Đây là
  15 phút mua lại nhiều giờ debug.
- **Với tính năng phụ thuộc dashboard bên ngoài**: bảng hợp đồng + bảng chẩn đoán ngược
  ([Playbook §4](FAILURE_FIRST_PLAYBOOK.md)).

### 🚧 Cổng 1

- [ ] Mỗi REQ có tiêu chí nghiệm thu kiểm chứng được
- [ ] Mỗi tính năng có I/O đều có bảng thất bại; **không dòng nào ghi "người dùng không thấy gì"**
- [ ] Mỗi phụ thuộc bên ngoài có bảng hợp đồng
- [ ] Có Risk Register, mỗi rủi ro có phương án

---

# PHA 2 — Nền móng dự án

Làm một lần, sai thì đau về sau.

1. **Namespace + applicationId** — đổi sau khi đã publish là **không thể**; Play khoá vĩnh viễn
   package name. Chốt theo thương hiệu dài hạn, không theo tên tạm.
2. **Keystore release** — tạo, **sao lưu ra ngoài máy**, ghi lại mật khẩu ở nơi an toàn. Mất keystore
   = mất quyền cập nhật app vĩnh viễn.
3. **Bí mật không vào git** — API key đọc từ `local.properties` (git-ignored) hoặc biến môi trường,
   nhúng lúc build. Không hardcode.
4. **Quy tắc version** — chốt và tự động hoá bằng script, cấm sửa tay.
   > Bẫy đã dẫm: bản phát hành ghi `versionName 0.1.0` với `versionCode 4`, hai số trôi khỏi nhau.
   > Luật hiện tại: `versionName` luôn là `0.0.<versionCode>`, chỉ script được ghi.
5. **Lệnh chuẩn** — build/test/lint/install ghi vào `CLAUDE.md` để mọi phiên làm việc dùng chung.
6. **Upload sớm một bản rỗng lên internal testing.** Play **không cho tạo in-app product** khi app
   chưa từng có bản nào trên track. Làm sớm để Pha 5 không bị chặn.

### 🚧 Cổng 2

- [ ] `applicationId` là tên dài hạn, đã kiểm tra chưa ai chiếm
- [ ] Keystore đã sao lưu **ra ngoài máy này**
- [ ] `git status` sạch bí mật; đã grep xác nhận key không nằm trong lịch sử
- [ ] Version do script quản, đã chạy thử
- [ ] Đã có ít nhất 1 bản trên internal testing

---

# PHA 3 — Vòng lặp tính năng

Lặp cho từng tính năng. Đây là nơi tiêu 80% thời gian.

```
Bảng thất bại → Code → Test → Chạy trên đúng loại build → Soát edge case → Xong
```

**1. Bảng thất bại trước.** Không code trước.

**2. Code theo luật** ([Playbook §2](FAILURE_FIRST_PLAYBOOK.md)):
- Luật quyết định ở hàm thuần top-level, không chôn trong ViewModel
- Rỗng từ nguồn xa = lỗi
- Boolean có phạm vi phải nhận phạm vi làm tham số
- Không `?: return` im lặng trong handler người dùng

**3. UI phải có ảnh mockup.** Không tự bịa giao diện. 1 ảnh = 1 màn = 1 vòng lặp. Tầng dữ liệu thì
không cần.

**4. Soát 6 nhóm edge case** — bắt buộc trước khi nói "xong":

| Nhóm | Hỏi gì |
|---|---|
| Rỗng / null | Danh sách rỗng? Trường thiếu? Lần chạy đầu chưa có dữ liệu? |
| Biên | 0, 1, phần tử cuối, vượt giới hạn, số âm |
| Mạng | Mất mạng, chậm, timeout, phản hồi méo |
| Đồng thời | Bấm hai lần, xoay màn hình, huỷ giữa chừng, chạy nền |
| Quyền | Từ chối, từ chối vĩnh viễn, thu hồi khi đang chạy |
| Ngôn ngữ / vùng | Chuỗi dài, RTL, số/ngày theo vùng, đổi ngôn ngữ giữa chừng |

**5. Chạy trên đúng loại build.** Xem [Pha 9](#pha-9--kiểm-thử-trước-khi-bấm-publish).

### 🚧 Cổng 3 (cho mỗi tính năng)

- [ ] Mọi dòng bảng thất bại đã **tự tay kích hoạt thử**, không phải đọc code suy ra
- [ ] Luật quyết định có unit test
- [ ] 6 nhóm edge case đã soát
- [ ] Lint sạch
- [ ] Đã chạy thật trên máy, không chỉ compile

---

# PHA 4 — Nội dung và asset

Áp dụng cho app nhiều media (audio, ảnh, bài học).

1. **Một nguồn chân lý cho nội dung** — file/DB sinh ra mọi thứ khác, không copy-paste rải rác.
2. **Pipeline sinh nội dung bằng script**, không thao tác tay. Repo này: `build_curriculum_json.py`
   → `optimize_app_audio.py` → `build_content_manifest.py` → `publish_content.py`.
   Sửa/thêm/xoá file trong pack **đã publish** là việc riêng, có bẫy riêng:
   [CONTENT_PACK_OPERATIONS.md](CONTENT_PACK_OPERATIONS.md).
3. **Quyết định sớm: nhúng trong app hay tải về.** Đổi sau sẽ đụng cả kiến trúc lẫn kích thước gói.
4. **Test 1 mẫu trước khi chạy hàng loạt.** Thao tác sửa tại chỗ không có bản lùi.
5. **Kiểm nội dung bằng tai/mắt người**, không bằng API tự động. Máy không phán được phát âm hay hay dở.

### 🚧 Cổng 4

- [ ] Nội dung sinh lại được từ script, không có bước tay không ghi lại
- [ ] Đã test 1–5 mẫu trước khi chạy hàng loạt
- [ ] Kích thước gói đã đo **sau khi build thật**, không ước lượng

> **Bẫy đã dẫm**: strip nội dung khỏi app xong, build ra APK **byte-identical**, không warning nào.
> Compose Resources copy vào thư mục generated và **không xoá file đã biến mất khỏi nguồn**. Luôn đo
> lại kích thước từ chính file build ra.

---

# PHA 5 — Kiếm tiền

Toàn bộ chi tiết ở [04-BILLING_SETUP.md](../abc-phonics-kids/04-BILLING_SETUP.md). Ba điều thuộc về
workflow:

1. **Làm sớm hơn bạn nghĩ.** Product mới Activate mất vài phút tới vài giờ mới mua được; quyền
   Service Account mất tới ~36 giờ để propagate. Không phải việc làm đêm trước khi publish.
2. **Định danh khớp cấu hình ngoài phải ghim bằng test.** Sai một ký tự = hỏng im lặng.
3. **Chỉ bản release mới chạm billing thật.** Emulator và bản debug không kết luận được gì.

### 🚧 Cổng 5

- [ ] Mua thật thành công trên **máy thật**, tài khoản tester, cài **từ Play track**
- [ ] Khôi phục giao dịch chạy đúng — kể cả khi chưa mua gì
- [ ] Mua rồi gỡ app cài lại, quyền vẫn còn
- [ ] Sở hữu A không mở khoá nhầm B

---

# PHA 6 — Tuân thủ và quyền riêng tư

Bỏ qua pha này thì bị **từ chối lúc duyệt**, tức là mất cả tuần chứ không phải một buổi.

1. **Privacy Policy + Terms** — phải có **URL công khai** trước khi submit.
2. **Khai báo Data safety** phải khớp **thực tế app làm gì**, không phải ý định. Play có đối chiếu.
3. **App cho trẻ em** — ràng buộc riêng và nghiêm: tắt Advertising ID nếu không dùng, không thu thập
   PII, quảng cáo phải phù hợp lứa tuổi, cần cổng phụ huynh cho hành vi mua bán.
4. **Quyền trong manifest** — mỗi quyền phải giải thích được. Quyền thừa kéo theo câu hỏi lúc duyệt.

### 🚧 Cổng 6

- [ ] Privacy Policy + Terms đã online, URL mở được ở chế độ ẩn danh
- [ ] Data safety khớp thực tế, đã đối chiếu với code chứ không điền theo trí nhớ
- [ ] Nếu app trẻ em: cổng phụ huynh chặn mọi hành vi mua bán, đã tự thử
- [ ] Đã rà từng quyền trong manifest, bỏ quyền không dùng
- [ ] **AAB đã build lại sau khi sửa manifest** — sửa file không tự vào gói

---

# PHA 7 — Trang cửa hàng

1. **Phạm vi locale = phạm vi đã chốt ở Pha 0.**
2. **Bản nháp trong repo ≠ bản đang chạy trên store.** Nội dung sửa thẳng trên console không bao giờ
   quay về repo.
   > Bẫy đã dẫm: bản EN đang chạy mở đầu bằng câu chưa từng có trong `listing.md`. Luật hiện tại:
   > **chụp lại bản live trước khi sửa bản nháp** (`marketing/store-listing/fetch-live.py`).
3. **Asset** — icon, ảnh chụp màn hình, feature graphic, video. Có checklist kích thước riêng.
   Video có luật riêng và dễ mất công vô ích: [../../marketing/video-production.md](../../marketing/video-production.md).
4. **Từ khoá** — theo dõi thứ hạng, không đặt một lần rồi quên.

### 🚧 Cổng 7

- [ ] Mỗi locale trong phạm vi có đủ mô tả + ảnh
- [ ] Đã chụp bản live trước khi sửa bản nháp
- [ ] Ảnh chụp màn hình lấy từ **bản build sắp ship**, không phải bản cũ
- [ ] Nếu có video: 16:9 (không phải Short), Public, tắt monetization, bật embedding, Made for kids
- [ ] Xếp hạng nội dung đã điền

---

# PHA 8 — Dựng bản phát hành

Dùng skill `/release`. Trình tự cứng:

```
Preflight → Bump → Build → Báo cáo
```

**Preflight — hỏng cái nào dừng cái đó:**

```bash
git status --porcelain    # thứ gì trong cây làm việc là thứ sẽ ship
./gradlew test            # bản phát hành có test đỏ không phải bản phát hành
./gradlew detekt --continue
grep -c RELEASE_STORE_PASSWORD local.properties
```

Test đỏ và thiếu chữ ký là **chặn cứng**. Cây làm việc bẩn thì không chặn, nhưng phải nói rõ đang
bẩn cái gì — vì chính nó sẽ ship.

### 🚧 Cổng 8

- [ ] Preflight xanh cả bốn
- [ ] Version bump đúng luật, đã dry-run
- [ ] Đã báo đường dẫn **và kích thước** file, không chỉ nói "xong"
- [ ] Kích thước không tăng bất thường so với bản trước — tăng thì phải giải thích được

---

# PHA 9 — Kiểm thử trước khi bấm publish

Pha bị cắt xén nhiều nhất, và là pha đắt nhất khi cắt.

**Luật số một:**

> Kiểm thử trên **đúng loại build sẽ ship**, và trên **đúng loại thiết bị người dùng có**.
>
> Bản debug thường thay dịch vụ thật bằng đồ giả. Emulator thiếu năng lực thật của máy. Kết luận
> "chạy tốt" rút ra từ chúng là kết luận **về thứ khác**.

**Ma trận tối thiểu:**

| Chiều | Phải phủ |
|---|---|
| Loại build | Chính xác bản sẽ upload |
| Cách cài | Từ Play track, không sideload |
| Thiết bị | Máy thật; ít nhất 1 máy đời thấp |
| Trạng thái | Cài mới **và** nâng cấp từ bản đang chạy |
| Mạng | Có, không, và chập chờn |
| Vùng | Mỗi locale trong phạm vi |

**Đừng quên đường nâng cấp.** Người dùng hiện tại không cài mới — họ cập nhật đè. Dữ liệu cũ, cache
cũ, schema cũ phải sống sót.

### 🚧 Cổng 9

- [ ] Đã chạy bản sắp ship, cài từ Play track, trên máy thật
- [ ] Đã thử nâng cấp từ bản đang chạy, dữ liệu cũ còn nguyên
- [ ] Luồng mua bán chạy đầu-cuối bằng tài khoản tester
- [ ] Thử offline / mất mạng giữa chừng
- [ ] Kiểm mỗi locale trong phạm vi

---

# PHA 10 — Publish và sau publish

1. **Phát hành theo bậc.** Đừng 100% ngay. Bắt đầu nhỏ, xem crash-free, rồi mở dần.
2. **Xem số trong 48 giờ đầu** — crash-free rate, ANR, đánh giá 1 sao. Đây là lúc lỗi thật lộ ra.
3. **Chụp lại bản listing live** vào repo sau khi publish, để bản nháp không trôi.
4. **Ghi lại bẫy vừa gặp** vào doc tương ứng. Đây là bước hay bị bỏ nhất và là bước làm workflow dày
   lên theo thời gian.

### 🚧 Cổng 10

- [ ] Rollout theo bậc, không phải 100%
- [ ] Có người thật sự nhìn số trong 48 giờ đầu
- [ ] Listing live đã chụp về repo
- [ ] Bẫy mới đã ghi vào doc, bảng chẩn đoán ngược dày thêm

---

## Bảng bẫy — đã trả giá thật ở project này

| Bẫy | Hậu quả | Chặn ở pha |
|---|---|---|
| Giá tồn tại 3 con số khác nhau, không cái nào là thật | Không biết điền gì lên store | 0 |
| Mọi đường hỏng đều im lặng | Người dùng bấm nút, không gì xảy ra | 1, 3 |
| `versionName` và `versionCode` trôi khỏi nhau | Không biết bản nào là bản nào | 2 |
| Strip nội dung nhưng build ra file y hệt | Tưởng đã giảm dung lượng, thực ra không | 4 |
| Biến thể "để test billing" dùng billing giả | Test xanh giả | 5, 9 |
| Log tắt ở bản release | Mù hoàn toàn đúng lúc cần nhất | 3 |
| Sửa manifest nhưng không build lại AAB | Bản upload vẫn mang lỗi cũ | 6 |
| Bản nháp listing trôi khỏi bản live | Mô tả trong repo nói dối | 7 |
| Kiểm thử trên emulator rồi kết luận | Emulator không làm được billing | 9 |

---

## Một câu tóm tắt

> Kế hoạch không cứu bạn khỏi lỗi. **Cổng** mới cứu — vì mỗi cổng là một lần ai đó đã trả giá, viết
> lại thành câu hỏi phải trả lời trước khi được đi tiếp.
