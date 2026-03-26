# Error Handling Reference — Grabee

## suspendRunCatching

The primary error handling utility. Located in `core/common/src/commonMain/.../CoroutineUtils.kt`:

```kotlin
suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellationException: CancellationException) {
    throw cancellationException  // never swallow cancellation
} catch (exception: Exception) {
    Napier.i(exception, "suspendRunCatching") { "Failed..." }
    Result.failure(exception)
}
```

Always use this instead of bare `try/catch` in ViewModels. It re-throws `CancellationException` (required for structured concurrency) and logs failures via Napier automatically.

---

## ScreenState.Error

Errors shown to users are represented as `ScreenState.Error` with a `StringResource` — never raw strings.

```kotlin
// In ViewModel (Pattern B)
_screenState.value = suspendRunCatching {
    repository.fetchData()
}.fold(
    onSuccess = { ScreenState.Idle(it) },
    onFailure = { ScreenState.Error(Res.string.error_network) },
)
```

`ScreenState.Error` full signature:
```kotlin
data class Error(
    val message: StringResource,
    val retryTitle: StringResource? = null,
    val throwable: Throwable? = null,
) : ScreenState<Nothing>()
```

`AsyncLoadContents` renders `ErrorView` automatically when state is `ScreenState.Error`. Pass `retryAction` to show a retry button:

```kotlin
AsyncLoadContents(
    screenState = screenState,
    retryAction = viewModel::fetch,                         // retry button on error
    terminate = { navBackStack.removeLastOrNull() },        // close button on error
) { uiState -> ... }
```

---

## Common Error String Keys

Defined in `core/resource/src/commonMain/composeResources/values/strings.xml`:

| Key | English message |
|---|---|
| `error_network` | "A network error occurred" |
| `error_network_description` | "Please check your connection and try again." |
| `error_unknown` | "An unknown error occurred" |
| `error_executed` | "An error occurred" |
| `error_billing` | "Failed to retrieve purchase information" |
| `error_download` | "Download failed" |
| `error_no_data` | "No data available" |

Usage:
```kotlin
ScreenState.Error(Res.string.error_network)
ScreenState.Error(
    message = Res.string.error_network,
    retryTitle = Res.string.common_retry,
)
```

---

## Action Errors (non-ScreenState)

For user-triggered actions (purchase, submit), use a separate sealed interface and show errors via `SnackbarHostState`. Pattern from `PaywallScreen.kt`:

```kotlin
// In Screen
LaunchedEffect(actionState) {
    when (actionState) {
        is XyzActionState.Error -> snackbarHostState.showSnackbar(actionState.message)
        else -> {}
    }
}
```

---

## Logging

Always use `Napier` — never `println()` or Android's `Log`:

```kotlin
import io.github.aakira.napier.Napier

Napier.d("Debug message")
Napier.i("Info message")
Napier.w("Warning message")
Napier.e("Error message", throwable)
```

Napier is initialized per platform:
- **Android**: `Napier.base(DebugAntilog())` in `GrabeeApplication`
- **iOS**: `initNapier()` in `InitHelper.kt` — called from Swift before `initKoin()`
