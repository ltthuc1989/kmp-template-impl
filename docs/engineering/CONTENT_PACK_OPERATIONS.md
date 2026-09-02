# Sửa nội dung audio/ảnh — công thức

Nội dung sau paywall không nằm trong app mà tải từ mạng về. Muốn sửa thì **chỉ cần chạy
script** — không bao giờ phải mở `content_manifest.json` ra sửa tay, không phải nhớ thứ tự gì.

Ba việc, mỗi việc một công thức. Chép, dán, xong.

---

## Việc 1 — Thay một file đọc sai

Ví dụ từ "jet" đọc sai, đã thu lại bản mới.

```bash
# Chép file mới vào ĐÚNG chỗ file cũ
cp ban-moi.mp3 core/resource/src/commonMain/composeResources/files/audio/level_1/unit_04/L1U04_J_jet/vocab/01_jet.mp3

# Chạy 2 lệnh
python3 scripts/optimize_app_audio.py
python3 scripts/publish_content.py --packs L1U4 --out build/cdn --upload --strip
```

Rồi **build bản app mới**.

---

## Việc 2 — Thêm file mới

Giống hệt Việc 1. Chép file vào đúng thư mục, chạy đúng 2 lệnh đó.

---

## Việc 3 — Bỏ hẳn một bài

```bash
python3 scripts/build_content_manifest.py --drop audio/level_1/unit_04/L1U04_J_jet/
```

Rồi **build bản app mới**.

---

## Chỗ duy nhất phải tự nghĩ: điền gì vào `--packs`

Nhìn đường dẫn file là ra — level mấy, unit mấy:

| File nằm ở | `--packs` điền |
|---|---|
| `audio/level_1/unit_04/...` | `L1U4` |
| `audio/level_2/unit_07/...` | `L2U7` |
| `audio/level_3/unit_05/...` | `L3U5` |

Nhiều file cùng lúc vẫn **một lệnh** — chép hết vào rồi chạy. Khác pack thì liệt kê bằng dấu
phẩy: `--packs L2U5,L2U6`.

Riêng **2 unit đầu mỗi level** thì không cần publish gì cả: chúng nằm sẵn trong app (hàng miễn
phí). Sửa file xong build lại là xong.

---

## Hai điều phải nhớ

**1. File mới phải đặt đúng chỗ file cũ.** Đặt sai thư mục là hỏng — script không đoán hộ được.

**2. Sửa xong phải ra bản app mới.** Đẩy lên mạng thôi thì không ai thấy gì. Bảng mục lục nội
dung nằm bên trong app, nên app cũ vẫn đọc theo bảng cũ.

Còn lại script tự lo. Làm sai chỗ nào nó **báo lỗi và dừng**, không im lặng làm bậy.

---

## Kiểm sau khi làm

```bash
python3 scripts/build_content_manifest.py --check
```

Ra `content_manifest.json khớp` là ổn.

---
---

# Phần dưới: chỉ đọc khi cần

Không cần đọc để dùng. Để đây cho lúc gặp lỗi lạ, hoặc lúc cần sửa chính mấy script này.

## Đọc output của lệnh publish

```
Đã dựng 1 file (0.0 MB) vào build/cdn/content
  bỏ qua 25 file đã strip từ trước       ← 25 file kia không đổi: đúng
Đang đẩy build/cdn/content → gs://abc-phonics-kids-content/content
Đã cập nhật content_manifest.json sau khi upload xong
```

Con số "Đã dựng N file" phải bằng đúng số file vừa thay. Lớn hơn = có file lạ lọt vào.

## Lệnh publish đụng vào những gì

| Việc | Cụ thể |
|---|---|
| Tạo mới | `build/cdn/content/<hash>/...` — cây file để đẩy lên mạng |
| Sửa | `content_manifest.json` |
| Xoá | file của pack vừa publish, trong `composeResources` (do `--strip`) |
| Xoá | `core/resource/build/generated/assets` + `intermediates/assets` |
| Đẩy lên mạng | `gs://abc-phonics-kids-content/content` (do `--upload`) |

Không đụng: code Kotlin, `curriculum.json`, pack khác, và 2 unit miễn phí mỗi level.

Bỏ `--strip` thì không xoá file nào — nhưng nội dung vẫn nằm trong app, tức không giảm được MB.
Bỏ `--upload` thì không đẩy lên mạng, và script sẽ **cảnh báo** rằng anh phải tự đẩy ngay.

## Vì sao xoá phải có lệnh riêng

Pack đã publish thì file **không còn trong repo** — `--strip` đã xoá đi rồi, đó là cả mục đích
của nó. Nên "file không có trên đĩa" là chuyện bình thường, không thể dùng nó để ra hiệu "tôi
muốn bỏ bài này". Phải nói thẳng bằng `--drop`.

`--drop` nhận **tiền tố đường dẫn**, nhiều cái cách nhau dấu phẩy. Gõ sai tiền tố thì script
báo lỗi chứ không im lặng bỏ qua. File cũ trên mạng vẫn để nguyên — bản app cũ còn đang đọc.

Chỉ `--drop` khi bài đó đã thôi được nhắc tới trong `curriculum.json` / `stories/`. Script không
kiểm hộ được.

## Vì sao sửa và thêm dùng chung một lệnh

Với script thì hai việc đó là một: quét đĩa, thấy file, băm ra mã, ghi vào bảng mục lục. Có sẵn
dòng cũ thì gọi là sửa, chưa có thì gọi là thêm.

## Bảng mục lục (`content_manifest.json`)

Sinh **hoàn toàn bằng script**, không sửa tay. `publish_content.py` cũng chỉ gọi vào
`build_content_manifest.py` chứ không tự ghi.

Nhưng nó vừa là **đầu ra** vừa là **đầu vào**: file của pack đã publish không còn trên đĩa để mà
quét, nên script đọc lại chính bảng cũ để chép tiếp. Hệ quả: **xoá file này đi rồi chạy lại
script là mất sạch**, không lấy lại được từ đĩa.

## Bẫy

| Bẫy | Hậu quả | Đã chặn bằng |
|---|---|---|
| Bảng mục lục trỏ ra mạng trước khi file lên mạng | bài đã bán câm, không lỗi không log | `--upload`: chỉ ghi bảng sau khi đẩy xong |
| Chạy `build_content_manifest.py --externalize none` | xoá sạch dòng của pack đã publish | mặc định là `keep` |
| Xoá file khỏi đĩa để "gỡ" bài | dòng cũ sống lại mỗi lần chạy script | `--drop` |
| Ghi đè file đã có trên mạng | phá lời hứa "đường dẫn không bao giờ đổi" | nội dung đổi → mã đổi → đường dẫn mới, không bao giờ đè |
| Strip xong build lại, app không nhỏ đi | file build ra y hệt, không cảnh báo | script tự dọn 2 thư mục build; vẫn phải đo từ file thật |
| Sửa audio nhưng không ra bản app mới | không ai thấy gì | — nhớ mà làm |

## Publish lần đầu một pack

Giống hệt sửa file, nhưng có hai thứ phải làm trước.

**1. Nén audio xong hẳn rồi mới publish.** Chạy `optimize_app_audio.py` trước, luôn luôn. Nén
sau khi publish thì mọi file đều đổi mã nội dung → phải publish lại từ đầu, và bản chưa nén đã
đẩy lên nằm lại trên mạng vĩnh viễn chẳng ai dùng. Kiểm còn file nào chưa nén:

```bash
python3 scripts/optimize_app_audio.py --dry-run     # "sẽ encode: 0 file" là xong
```

**2. Audio phải vào git trước.** `--strip` xoá thật; chưa commit thì không có đường lùi.

## Soi thử bằng server local, không cần mạng

```bash
python3 scripts/publish_content.py --packs L1U4 --out /tmp/cdn
python3 -m http.server 8000 --directory /tmp/cdn
# local.properties: CONTENT_CDN_BASE_URL=http://10.0.2.2:8000/content
```

## Kiến trúc

Vì sao chia pack theo unit, vì sao mã nội dung nằm trong đường dẫn, cấu hình bucket:
[APP_DELIVERY_WORKFLOW.md](APP_DELIVERY_WORKFLOW.md) PHA 4.
