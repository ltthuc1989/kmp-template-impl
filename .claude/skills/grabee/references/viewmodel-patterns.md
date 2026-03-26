# ViewModel Patterns — Grabee

Two patterns exist in the codebase. Choose based on the screen's needs.

---

## Pattern A — Simple Pass-Through

**When to use:** ViewModel forwards a repository `StateFlow` and dispatches write actions. No async loading state needed.

**Real example:** `feature/setting/SettingViewModel.kt`

```kotlin
class SettingViewModel(
    private val repository: AppSettingRepository,
) : ViewModel() {

    // Expose repository StateFlow directly
    val setting = repository.setting

    fun setTheme(theme: Theme) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    fun setUseDynamicColor(useDynamicColor: Boolean) {
        viewModelScope.launch { repository.setUseDynamicColor(useDynamicColor) }
    }

    fun setSeedColor(color: Color) {
        viewModelScope.launch { repository.setSeedColor(color) }
    }
}
```

**In Screen:**
```kotlin
@Composable
fun SettingScreen(modifier: Modifier = Modifier) {
    val viewModel: SettingViewModel = koinViewModel()
    val setting by viewModel.setting.collectAsStateWithLifecycle()

    SettingContent(
        setting = setting,
        onThemeChanged = viewModel::setTheme,
        modifier = modifier,
    )
}
```

---

## Pattern B — Async Load + Action Feedback

**When to use:** Screen loads async data (Loading → Idle/Error) AND has user actions with separate feedback states.

**Real example:** `feature/billing/PaywallViewModel.kt`

```kotlin
class PaywallViewModel(
    private val billingRepository: BillingRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<PaywallUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<PaywallUiState>> = _screenState.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseUiState>(PurchaseUiState.Idle)
    val purchaseState: StateFlow<PurchaseUiState> = _purchaseState.asStateFlow()

    private val _selectedPlan = MutableStateFlow(SubscriptionPlan.YEARLY)
    val selectedPlan: StateFlow<SubscriptionPlan> = _selectedPlan.asStateFlow()

    init { fetch() }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = ScreenState.Loading()
            _screenState.value = suspendRunCatching {
                val products = billingRepository.getProducts() ?: emptyList()
                PaywallUiState(products = products.toImmutableList())
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }

    fun purchase() {
        viewModelScope.launch {
            _purchaseState.value = PurchaseUiState.Loading
            when (val result = billingRepository.purchase(product)) {
                PurchaseResult.Success -> _purchaseState.value = PurchaseUiState.Success
                PurchaseResult.Cancelled -> _purchaseState.value = PurchaseUiState.Idle
                is PurchaseResult.Error -> _purchaseState.value = PurchaseUiState.Error(result.message)
            }
        }
    }
}

@Stable
data class PaywallUiState(
    val products: ImmutableList<ProductInfo>,
)

@Stable
sealed interface PurchaseUiState {
    data object Idle : PurchaseUiState
    data object Loading : PurchaseUiState
    data object Success : PurchaseUiState
    data object PurchaseFailed : PurchaseUiState
    data object NoSubscriptionToRestore : PurchaseUiState
    data class Error(val message: String) : PurchaseUiState
}
```

**In Screen:**
```kotlin
@Composable
fun PaywallScreen(modifier: Modifier = Modifier) {
    val viewModel: PaywallViewModel = koinViewModel()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val purchaseState by viewModel.purchaseState.collectAsStateWithLifecycle()
    val selectedPlan by viewModel.selectedPlan.collectAsStateWithLifecycle()

    AsyncLoadContents(
        screenState = screenState,
        retryAction = viewModel::fetch,
        modifier = modifier,
    ) { uiState ->
        PaywallContent(
            uiState = uiState,
            purchaseState = purchaseState,
            selectedPlan = selectedPlan,
            onSelectPlan = viewModel::selectPlan,
            onPurchase = viewModel::purchase,
        )
    }
}
```

---

## UiState Rules

Always apply to UiState and action-state classes:
- `@Stable` annotation on data classes and sealed interfaces
- `ImmutableList<T>` (from `kotlinx-collections-immutable`) for list fields
- Never expose `MutableStateFlow` — always call `.asStateFlow()`

```kotlin
@Stable
data class MyUiState(
    val items: ImmutableList<Item>,  // not List<Item>
    val title: String,
)
```

---

## suspendRunCatching

Located in `core/common/src/commonMain/.../CoroutineUtils.kt`. Wraps a suspend block in `Result`, re-throws `CancellationException` (required for structured concurrency), and logs failures via Napier.

```kotlin
// Usage in ViewModel
_screenState.value = suspendRunCatching {
    repository.fetchData()
}.fold(
    onSuccess = { ScreenState.Idle(it) },
    onFailure = { ScreenState.Error(Res.string.error_network) },
)
```

---

## Koin Registration

```kotlin
// di/XyzModule.kt
val xyzModule = module {
    viewModelOf(::XyzViewModel)  // auto-generates ViewModel factory
}
```

Register in `composeApp/src/commonMain/.../di/Koin.kt`:
```kotlin
fun KoinApplication.applyModules() {
    // ... existing modules ...
    modules(xyzModule)
}
```
