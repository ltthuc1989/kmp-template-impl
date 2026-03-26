# Testing Reference — Grabee

## Test Infrastructure

Test dependencies are declared in `core:datasource/build.gradle.kts` under `commonTest`:

```kotlin
commonTest.dependencies {
    implementation(kotlin("test"))
    implementation(libs.ktor.client.mock)
    implementation(libs.kotlinx.coroutines.test)
}
```

Tests live in `src/commonTest/kotlin/` within the module being tested.

---

## Test Doubles — Fakes over Mocks

Grabee does not use mocking libraries. Write fake implementations of repository interfaces instead:

```kotlin
// Fake repository for testing
class FakeAppSettingRepository : AppSettingRepository {
    private val _setting = MutableStateFlow(AppSetting.DEFAULT)
    override val setting: StateFlow<AppSetting> = _setting.asStateFlow()

    override suspend fun setTheme(theme: Theme) {
        _setting.value = _setting.value.copy(theme = theme)
    }
}
```

Place fakes in `src/commonTest/kotlin/.../fake/`.

---

## ViewModel Tests

Use `kotlinx-coroutines-test` with `StandardTestDispatcher`:

```kotlin
class SettingViewModelTest {

    @Test
    fun `setTheme updates setting`() = runTest {
        val fakeRepo = FakeAppSettingRepository()
        val viewModel = SettingViewModel(fakeRepo)

        viewModel.setTheme(Theme.Dark)
        advanceUntilIdle()

        assertEquals(Theme.Dark, viewModel.setting.value.theme)
    }
}
```

For Pattern B ViewModels (with `ScreenState`), assert state transitions:

```kotlin
@Test
fun `fetch emits Loading then Idle`() = runTest {
    val fakeRepo = FakeBillingRepository()
    val viewModel = PaywallViewModel(fakeRepo)

    // init{} calls fetch() — starts as Loading
    assertTrue(viewModel.screenState.value is ScreenState.Loading)

    advanceUntilIdle()

    assertTrue(viewModel.screenState.value is ScreenState.Idle)
}
```

---

## Ktor Mock Client

For datasource tests, use `MockEngine` from `ktor-client-mock`:

```kotlin
val mockEngine = MockEngine { request ->
    respond(
        content = """{"key": "value"}""",
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
}

val client = HttpClient(mockEngine) {
    install(ContentNegotiation) { json() }
}
```

---

## Running Tests

```bash
# All tests
./gradlew test

# Specific module
./gradlew :core:datasource:testDebugUnitTest
./gradlew :core:repository:testDebugUnitTest
```
