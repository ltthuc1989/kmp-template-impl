# UI From Screenshot — Compose Multiplatform

Reference cho `/grabee` khi user **attach UI mockup** (PNG/JPG/Figma export) trong prompt.

**Khi nào load**: trong message của user có image attachment, HOẶC user nói "design like this", "match this mockup", "implement this UI".

---

## Workflow bắt buộc

### Bước 1: Đọc image
**ALWAYS** dùng Read tool đọc file image trước khi viết code:
```
Read tool với file_path = đường dẫn image attached
```
Image sẽ hiển thị trực quan. Phân tích:

1. **Layout structure**: Column / Row / Box / LazyColumn / LazyVerticalGrid? Padding bao nhiêu? Alignment thế nào?
2. **Color palette**: extract 3-5 màu chính. Map vào MaterialTheme tokens (primary, secondary, tertiary, surface, onSurface...) hoặc kolor seed color. **KHÔNG hardcode hex** trừ khi không thể map.
3. **Typography**: identify font sizes (heading vs body), weights (regular/medium/bold). Map vào `MaterialTheme.typography.headlineLarge/titleMedium/bodyLarge/...`.
4. **Components**: Button (Filled / Outlined / Text)? Card? IconButton? FAB? BottomSheet? Dialog?
5. **Spacing**: padding/margin nhìn thấy. Multiples of 4dp (4/8/12/16/24/32/40/48). KHÔNG dùng 5/7/9 hoặc số lẻ.
6. **Touch targets**: cho kids app **≥ 64dp** (không 44pt như Apple HIG adult).
7. **Aspect ratios**: image cards 1:1, 4:3, 16:9?
8. **States visible**: focused/pressed/disabled state có hiển thị không?

### Bước 2: Verify ambiguous
Nếu screenshot không rõ:
- Hover/pressed state mà không thấy
- Color gradient phức tạp
- Custom font
- Animation timing

→ **HỎI user clarify**, KHÔNG đoán. Ví dụ: "Mockup không rõ pressed state của button. Dùng default ripple hay custom highlight?"

### Bước 3: Sinh code
Compose code dùng:
- `GrabeeTheme` tokens (`MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`).
- `Modifier.padding(MaterialTheme.spacing.medium)` nếu spacing system có sẵn, hoặc literal `16.dp`.
- `stringResource(Res.string.*)` cho text — KHÔNG hardcode user-facing string.
- `painterResource(Res.drawable.*)` cho image.
- Stateless composable + state hoist lên ViewModel.
- `modifier: Modifier = Modifier` là last non-lambda param.

### Bước 4: Self-checklist trước khi return
- [ ] Mọi element trong screenshot có trong code (không bỏ sót).
- [ ] Không hardcode color hex (trừ exception đã ghi chú).
- [ ] Không hardcode dimension không phải multiple of 4dp.
- [ ] Touch target ≥ 64dp cho action button.
- [ ] String dùng `stringResource(Res.string.*)`.
- [ ] Composable accept `modifier: Modifier`.
- [ ] Preview function `@Preview` ở dưới (nếu androidPreview module có).

---

## Color Mapping Cheatsheet

### Material3 ColorScheme tokens (Material3 Expressive)

| Token | Use case |
|---|---|
| `colorScheme.primary` | Primary CTA buttons, brand color |
| `colorScheme.onPrimary` | Text/icon on primary |
| `colorScheme.primaryContainer` | Subtle primary backgrounds (chips, cards) |
| `colorScheme.onPrimaryContainer` | Text on primaryContainer |
| `colorScheme.secondary` | Secondary actions |
| `colorScheme.tertiary` | Accent (gold stars, achievements) |
| `colorScheme.surface` | Card/sheet background |
| `colorScheme.onSurface` | Body text |
| `colorScheme.surfaceVariant` | Subtle dividers, disabled bg |
| `colorScheme.error` | Error states, destructive actions |
| `colorScheme.background` | Screen bg |

### kolor (Material You) — dynamic theming
GrabeeTheme dùng kolor để generate ColorScheme từ seed color (user setting). Không hardcode → tự động đổi theo theme.

### Khi nào hardcode hex
- Image overlay specific color (vd character mascot color).
- Brand asset không nằm trong theme.
- Ngay cả khi đó, define trong `core:resource/.../color.xml` hoặc theme extension.

---

## Typography Cheatsheet

| Token | Default size (Material3 Expressive) | Use |
|---|---|---|
| `typography.displayLarge` | 57sp | Hero text (rare) |
| `typography.headlineLarge` | 32sp | Screen title |
| `typography.headlineMedium` | 28sp | Section title |
| `typography.titleLarge` | 22sp | Card title |
| `typography.titleMedium` | 16sp | List item title |
| `typography.bodyLarge` | 16sp | **Body text default** |
| `typography.bodyMedium` | 14sp | Supporting text |
| `typography.labelLarge` | 14sp | Button text |
| `typography.labelMedium` | 12sp | Chip text |

**Kids app rule**: ưu tiên `bodyLarge` cho body (≥ 16sp), `headlineMedium` cho heading (≥ 28sp). Tránh `bodyMedium` (14sp) cho text trẻ đọc.

---

## Component Cheatsheet

```kotlin
// Filled button (primary CTA)
Button(onClick = { ... }, modifier = Modifier.heightIn(min = 64.dp)) {
    Text(stringResource(Res.string.action_continue))
}

// Outlined button (secondary)
OutlinedButton(onClick = { ... }) { Text(...) }

// Text button (tertiary, dialog)
TextButton(onClick = { ... }) { Text(...) }

// Icon button (toolbar action)
IconButton(onClick = { ... }, modifier = Modifier.size(64.dp)) {
    Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.cd_settings))
}

// FAB
FloatingActionButton(onClick = { ... }) { Icon(...) }

// Card
Card(modifier = Modifier.padding(16.dp)) {
    Column(modifier = Modifier.padding(16.dp)) { ... }
}

// AlertDialog
AlertDialog(
    onDismissRequest = { ... },
    title = { Text(...) },
    text = { Text(...) },
    confirmButton = { TextButton(onClick = { ... }) { Text(...) } },
)

// ModalBottomSheet
ModalBottomSheet(onDismissRequest = { ... }) { ... }

// LazyColumn / LazyRow / LazyVerticalGrid
LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(16.dp)) {
    items(list) { item -> ... }
}

// HorizontalPager (onboarding carousel)
val pagerState = rememberPagerState(pageCount = { 3 })
HorizontalPager(state = pagerState) { page -> ... }
```

---

## Example: Mockup → Code

**Mockup attached**: screenshot showing a level card with image, title, progress bar, "Continue" button.

```kotlin
@Composable
fun LevelCard(
    level: Level,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Image(
                painter = painterResource(level.iconResource),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(level.titleRes),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { level.progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
            ) {
                Text(stringResource(Res.string.action_continue))
            }
        }
    }
}
```

---

## Common Mistakes

❌ Hardcode `Color(0xFFFF6B6B)` thay vì `MaterialTheme.colorScheme.primary`.
❌ Hardcode `text = "Continue"` thay vì `stringResource(Res.string.action_continue)`.
❌ Touch button `Modifier.size(48.dp)` cho kids app — phải ≥ 64dp.
❌ `fontSize = 14.sp` cho body — kids cần ≥ 16sp, ưu tiên `bodyLarge`.
❌ Đoán hover/pressed state không có trong mockup — hỏi user clarify.
❌ Bỏ `modifier: Modifier = Modifier` parameter.
❌ State trong composable thay vì hoist lên ViewModel.
❌ Đọc image qua mô tả thay vì Read tool — phải gọi Read.

✅ Đọc image qua Read tool trước.
✅ Map color/typography vào theme tokens.
✅ Touch ≥ 64dp.
✅ Spacing multiples of 4dp.
✅ String resources EN + JA.
✅ Self-checklist trước khi return code.
✅ Hỏi clarify nếu mockup ambiguous.
