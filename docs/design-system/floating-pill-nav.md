# Floating Pill Nav Bar

A pill-shaped floating bottom navigation bar for consumer / kids KMP apps built with Compose Multiplatform + Material3 Expressive. The bar sits above the system navigation inset, fully rounded, with a primary-container chip highlighting the active tab.

This document is **self-contained and portable** — any KMP Compose project can lift the rule + reference component as-is.

---

## 1. Overview

**Use when**:
- 3–5 top-level root tabs on the main screen of a consumer / kids app.
- You want the nav bar visually detached from the system nav bar (floating look).
- You want a playful, high-visibility active state (kids / casual apps).

**Don't use when**:
- Nested tabs or sub-tabs inside a single screen (use `TabRow` / `SegmentedTabRow`).
- Navigation drawer / modal nav patterns.
- Tab count > 5 (labels overflow, tap targets shrink below 64dp).
- Full-immersive screens (video / game / camera viewfinder).

---

## 2. Anatomy

- **Container** — pill bar, fully rounded (`RoundedCornerShape(50)`), floating above the system navigation inset.
- **NavItem** — a single tab: icon (24dp) over label (`labelSmall` semibold). Stacked vertically.
- **Active indicator** — a `primaryContainer`-tinted pill chip wrapping the active `NavItem`, padding 20dp horizontal / 8dp vertical.
- **Elevation layer** — drop shadow below the container creating the float effect.

Layout (text diagram):

```
┌─────────────────────────────────────────────────────┐
│ ╭─────╮   │   │   │   │   │   │                     │  ← container (pill)
│ │ ⌂   │   📖     🎮     ⚙️                          │
│ │Home │   Lessons Games  Settings                   │
│ ╰─────╯                                             │  ← active chip = primaryContainer
└─────────────────────────────────────────────────────┘
                    ↕ 32dp
        ═════════ system nav bar ═════════
```

---

## 3. Visual tokens

All values below are the design source of truth. Component MUST consume them through `MaterialTheme.colorScheme.*` / `MaterialTheme.typography.*` only — **zero hex**.

| Token | Value | Why |
|---|---|---|
| Container shape | `RoundedCornerShape(50)` | Full pill (matches mockup) |
| Container width | `92%` of viewport, capped at `512dp` | Breathing room on tablet |
| Container min height | `64dp` | Kids touch target (from `ui-from-screenshot.md`) |
| Container inner padding | `8dp` all sides | Chip breathing room |
| Container background | `colorScheme.surface.copy(alpha = 0.9f)` | Glass effect |
| Container border | `1dp` `colorScheme.outline.copy(alpha = 0.2f)` | Crisp edge |
| Container shadow | `elevation = 12dp`, ambient `colorScheme.primary.copy(alpha = 0.15f)` | Float effect |
| Container bottom offset | `32dp` above navigation inset | Detach from system nav |
| Container top margin (relative to content) | `16dp` | Prevents overlap when content scrolls |
| Item horizontal padding (active) | `20dp` | Chip emphasis |
| Item horizontal padding (inactive) | `12dp` | Muted |
| Item vertical padding | `8dp` | 4dp multiple |
| Item icon size | `24dp` | M3 standard |
| Item icon–label gap | `2dp` | Tight visual pair |
| Item label style | `typography.labelSmall` + `FontWeight.SemiBold` | ~11sp, legible |
| Active item background | `colorScheme.primaryContainer.copy(alpha = 0.5f)` | Soft highlight |
| Active item icon | Filled variant, tint `colorScheme.primary` | Reinforces selection |
| Active item label color | `colorScheme.primary` + `FontWeight.SemiBold` | Emphasis |
| Inactive item color | `colorScheme.onSurfaceVariant` | Muted |
| Press scale | `0.9f` on pressed | Tactile feedback |
| Selection transition | `animateColorAsState` / `animateDpAsState`, `tween(200, FastOutSlowInEasing)` | Responsive |
| Press transition | `tween(150)` | Quick |

Spacing rule: **all values are multiples of 4dp** (2 / 4 / 8 / 12 / 16 / 20 / 24 / 32 / 64). Do not introduce odd values.

---

## 4. API contract

```kotlin
@Immutable
data class FloatingPillNavItem(
    val key: String,                // stable id, must be unique within items
    val label: StringResource,      // localized label (compose-resources)
    val iconInactive: ImageVector,  // outlined variant
    val iconActive: ImageVector,    // filled variant
)

@Composable
fun FloatingPillNavBar(
    items: ImmutableList<FloatingPillNavItem>,
    selectedKey: String,
    onItemSelected: (FloatingPillNavItem) -> Unit,
    modifier: Modifier = Modifier,
)
```

Rules:
- `items.size in 2..5`. Fewer than 2 defeats the purpose; more than 5 breaks layout at 92% width.
- `label` is mandatory. Never hide labels to save space — kids rely on text-icon pairing.
- `iconActive` must be a **filled** variant of `iconInactive` (e.g. `Icons.Outlined.Home` → `Icons.Filled.Home`). Reinforces selection.
- `selectedKey` must match one `items[i].key`. If it doesn't, no item is highlighted (no crash).
- `onItemSelected` fires on tap. Caller owns state — component is fully controlled.

---

## 5. Accessibility

Checklist (must all pass):

- Container has `Modifier.semantics { role = Role.TabBar; contentDescription = "Navigation" }` (localize `contentDescription` via parameter if consumed across projects).
- Every item exposes `contentDescription = stringResource(item.label)`.
- Effective touch target `≥ 64dp` — bigger than M3's default 48dp. Kids need generous targets.
- Icon + label always rendered together. Never hide label based on width or selection state.
- Active label vs active chip background contrast `≥ 4.5:1` (verify when you swap primary color).
- State changes announce via TalkBack / VoiceOver: `selected = (key == selectedKey)` on each item so screen readers say "selected".

---

## 6. Edge-to-edge & inset handling

KMP apps typically enable `enableEdgeToEdge()` on Android — the system nav bar no longer pushes content up automatically. The floating pill must handle its own inset.

**Consumed inside `Scaffold.bottomBar`** (recommended):
- `Scaffold` forwards `WindowInsets.navigationBars` into the bottom-bar slot.
- The component itself applies `.navigationBarsPadding()` internally so it renders above the system nav bar regardless of scaffolding.

**Consumed outside `Scaffold`** (not recommended but supported):
- Caller places it inside a `Box(contentAlignment = Alignment.BottomCenter)`.
- Component still `.navigationBarsPadding()` itself.
- Caller is responsible for adding `bottom padding` on the scrolling content to prevent the bar from covering the last item — recommended `72dp` (bar height + inset gap).

Top-bar side: unrelated — pair with a standard M3 `CenterAlignedTopAppBar` (auto status-bar inset) or another component that calls `.statusBarsPadding()`.

---

## 7. States & animation

| State | Trigger | Animation |
|---|---|---|
| Selected ↔ unselected | `selectedKey` change | `animateColorAsState` on background / text / icon tint (200ms, `FastOutSlowInEasing`); `animateDpAsState` on item horizontal padding (12dp ↔ 20dp) |
| Icon swap (outline ↔ filled) | Selection change | `AnimatedContent` with fade (150ms) — no layout shift |
| Press | Tap down | `Modifier.scale` via `animateFloatAsState(if (pressed) 0.9f else 1f, tween(150))` |
| Ripple | Inside chip area | Respect `indication = LocalIndication.current` — do not disable |

Never animate container `width` / `height` — the bar stays a stable size; only item internals animate. Animating the container causes layout shifts in surrounding content.

---

## 8. Theming & cross-project adaptation

- Component consumes only `MaterialTheme.colorScheme.*` (surface, primary, primaryContainer, onSurfaceVariant, outline) and `MaterialTheme.typography.*` (labelSmall).
- Dark mode: auto via `MaterialTheme.colorScheme` swap. No branch needed.
- Dynamic color (Material You / kolor): propagates automatically — if host app uses `rememberDynamicColorScheme`, bar adopts the user's seed color.
- Porting to a non-Grabee project: see §11.

---

## 9. Do / Don't

**Do**:
- ✅ Place inside `Scaffold.bottomBar` on root screens (Home, Review, Settings…).
- ✅ Provide outline + filled icon pair for every item.
- ✅ Keep labels visible at all times.
- ✅ Keep item count in the 2–5 range.
- ✅ Use theme tokens — `colorScheme.primary`, not `Color(0xFF0078D4)`.

**Don't**:
- ❌ Place it in a raw `Box` / `Column` without `.navigationBarsPadding()`.
- ❌ Use labels longer than ~10 characters — they overflow at 92% viewport on phones.
- ❌ Animate container height / width — only animate item internals.
- ❌ Add badges, FABs, or notification dots — those belong to a different component. Build a sibling variant if needed; don't extend this one.
- ❌ Reuse the same icon for inactive and active states — the filled/outlined contrast is load-bearing for kids.

---

## 10. Usage

```kotlin
val items = remember {
    persistentListOf(
        FloatingPillNavItem(
            key = "home",
            label = Res.string.nav_home,
            iconInactive = Icons.Outlined.Home,
            iconActive = Icons.Filled.Home,
        ),
        FloatingPillNavItem(
            key = "review",
            label = Res.string.nav_review,
            iconInactive = Icons.Outlined.MenuBook,
            iconActive = Icons.Filled.MenuBook,
        ),
        FloatingPillNavItem(
            key = "settings",
            label = Res.string.nav_settings,
            iconInactive = Icons.Outlined.Settings,
            iconActive = Icons.Filled.Settings,
        ),
    )
}

Scaffold(
    bottomBar = {
        FloatingPillNavBar(
            items = items,
            selectedKey = currentTabKey,
            onItemSelected = { item -> onTabChange(item.key) },
        )
    },
) { padding ->
    // screen content; respect `padding`
}
```

---

## 11. Porting to another KMP project

The reference file [core/ui/src/commonMain/.../screen/view/FloatingPillNavBar.kt](../../core/ui/src/commonMain/kotlin/me/ltthuc/kmp/core/ui/screen/view/FloatingPillNavBar.kt) is pure Compose commonMain code. To port:

1. Copy the file into your target module's `commonMain`. Rename the package.
2. Ensure the target module has these dependencies:
   - `androidx.compose.material3` (Expressive variant optional — the component does not hard-depend on it)
   - `androidx.compose.material:material-icons-extended` (or supply icons from your icon pack)
   - `org.jetbrains.kotlinx:kotlinx-collections-immutable`
   - `org.jetbrains.compose.components:components-resources` (if you keep `StringResource`; otherwise swap the `label` param to `String`)
3. Optional: replace `StringResource` with `String` and `stringResource(item.label)` with `item.label` if your project doesn't use `compose-resources`.
4. The component does not reference any project-specific types (no `BuildKonfig`, `AppSetting`, repositories). Grep the copy for unresolved imports after the rename.

---

## 12. Checklist for reviewers

When a PR introduces or modifies this component:

- [ ] No hardcoded hex colors — everything through `MaterialTheme`.
- [ ] Touch target ≥ 64dp.
- [ ] All spacing values are 4dp multiples.
- [ ] Every `FloatingPillNavItem` has both outline and filled icons.
- [ ] `.navigationBarsPadding()` applied inside the component, not left to the caller.
- [ ] Item count in 2..5 enforced (via `require` or lint note).
- [ ] Animations respect the 200ms / 150ms specs above.
- [ ] Labels are `StringResource`, not raw `String`.
- [ ] No project-specific imports leaked (component stays portable).
