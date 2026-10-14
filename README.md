# qa-kotlin-framework

[![CI](https://github.com/Obrams/qa-kotlin-framework/actions/workflows/tests.yml/badge.svg)](https://github.com/Obrams/qa-kotlin-framework/actions/workflows/tests.yml)
[![Allure Report](https://img.shields.io/badge/Allure-report-orange.svg)](https://obrams.github.io/qa-kotlin-framework/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
[![JUnit5](https://img.shields.io/badge/JUnit-5.10-green.svg)](https://junit.org/junit5)
[![Playwright](https://img.shields.io/badge/Playwright-1.40-purple.svg)](https://playwright.dev)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Problem:** API and UI test suites in JVM projects tend to grow into slow, serial, flaky runs with shared mutable state and no clear separation between the framework and the tests.

**Solution:** a small, opinionated Kotlin test framework where every test runs in parallel by default, test state is isolated per thread, HTTP responses are typed with a sealed result, and reporting, Docker and CI are wired in from day one.

**Stack:** Kotlin · JUnit 5 (parallel) · Spring WebClient · Playwright · kotlinx.serialization · Allure · ktlint · Gradle Kotlin DSL · Docker · GitHub Actions

Example targets used by the sample tests: [Swagger Petstore](https://petstore.swagger.io) (API) and [SauceDemo](https://www.saucedemo.com) (UI). Swap them for your own services — the framework code has no knowledge of either.

---

## What you get

| Area | What is in the box |
|------|--------------------|
| **API** | `ApiClient` on Spring WebClient; typed `get<T>` / `post<Req, Res>`; `ApiResponse` sealed class (`Success` / `HttpError` / `Error`), so a test never catches exceptions to branch on status |
| **UI** | Playwright Page Objects on top of `BaseScreen` + `ScreenElement`; every test gets its own Playwright instance and browser (Playwright for Java is not thread-safe); explicit `WaitConditions` instead of sleeps |
| **Parallelism** | JUnit 5 `concurrent` mode for classes and methods; `TestContext` (ThreadLocal) keeps per-test data, request IDs and metadata isolated; `ThreadSafeReportCollector` aggregates results across threads |
| **Data** | `TestDataFactory` for unique, collision-free entities; serializable DTOs |
| **Reporting** | Allure with steps/attachments via `AllureExtensions`; history is carried between CI runs and published to GitHub Pages |
| **Quality gates** | ktlint on every push; API and UI jobs run separately; Docker image build on `main` |

## Quick start

```bash
# prerequisites: JDK 21, Docker (optional)
./gradlew apiTest            # API tests only (@Tag("api"))
./gradlew uiTest             # UI tests only  (@Tag("ui"))
./gradlew test               # everything, in parallel
./gradlew allureServe        # open the local Allure report
```

With Docker:

```bash
docker compose up tests                  # run the suite
docker compose up -d allure allure-ui    # Allure server at http://localhost:5252
```

## Writing a test

API — the sealed response makes the happy path and the error path explicit:

```kotlin
@Tag("api")
class PetApiTests : BaseApiTest() {

    @Test
    fun `create and read pet`() {
        val pet = TestDataFactory.createPet()

        when (val created = apiClient.post<Pet, Pet>("/pet", pet)) {
            is ApiResponse.Success   -> assertThat(created.data.name).isEqualTo(pet.name)
            is ApiResponse.HttpError -> fail("HTTP ${created.statusCode}: ${created.body}")
            is ApiResponse.Error     -> fail(created.exception)
        }
    }
}
```

UI — Page Objects return the next page, so flows read top to bottom:

```kotlin
class LoginPage(page: Page) : BaseScreen(page) {
    private val username = css("#user-name")
    private val password = css("#password")
    private val submit   = css("#login-button")

    fun login(user: String, pass: String): InventoryPage {
        username.clearAndType(user)
        password.clearAndType(pass)
        submit.click()
        return InventoryPage(page)
    }
}
```

Per-test state lives in `TestContext` and is created/destroyed by the base classes:

```kotlin
TestContext.initContext(testName = "create pet")   // @BeforeEach in BaseApiTest / BaseUiTest
TestContext.trackRequestId(response.requestId)      // correlate API calls with the test
TestContext.cleanup()                               // @AfterEach
```

## Design decisions

- **Parallel by default, not opt-in.** `junit.jupiter.execution.parallel.*` is set in Gradle, and the framework is written so that nothing is shared between threads. The `concurrent/` tests exist specifically to catch regressions in that guarantee.
- **ThreadLocal context instead of static fields.** `TestContext` holds the test name, generated IDs, request IDs and metadata for the current thread only. A test can run 50-wide without leaking data into a neighbour.
- **Sealed `ApiResponse` instead of exceptions.** Non-2xx is a value, not a throw. Tests pattern-match on it, which keeps negative cases as readable as positive ones.
- **Spring WebClient rather than a test-only HTTP library.** Same client that the services under test typically use, so reactive timeouts, codecs and connection pooling behave the way they do in production.
- **One Playwright per test.** Playwright for Java is not thread-safe, so `BaseUiTest` creates a Playwright instance, browser and context for every test and closes them afterwards. It costs about a second of start-up per test and buys UI tests that run in parallel with nothing shared between threads.
- **Tags over directories for selection.** `@Tag("api")` / `@Tag("ui")` plus dedicated Gradle tasks (`apiTest`, `uiTest`) let CI run the two groups on separate runners without touching the source layout.
- **Allure history survives CI runs.** The report job checks out the previous `gh-pages` content and merges `history/` before generating, so trends and flakiness are visible across builds, not just inside one.
- **ktlint as a hard gate.** Lint runs first and every test job depends on it; style drift never reaches `main`.

## Project layout

```
src/main/kotlin/com/automation/
├── core/
│   ├── api/          ApiClient, ApiRequest, ApiResponse, UrlBuilder
│   ├── ui/           BaseScreen, ScreenElement, BrowserManager, WaitConditions
│   ├── threading/    TestContext, ThreadSafeReportCollector
│   ├── reporting/    AllureExtensions
│   └── config/       TestConfig
├── data/
│   ├── models/       Pet, Order, User
│   └── factories/    TestDataFactory
└── utils/            Extensions

src/test/kotlin/com/automation/
├── api/              Petstore API tests
├── ui/               SauceDemo and Practice page objects + tests
├── concurrent/       Thread-safety and parallel-execution tests
├── integration/      End-to-end flows
└── base/             BaseApiTest, BaseUiTest
```

## CI/CD

GitHub Actions ([`tests.yml`](.github/workflows/tests.yml)):

| Job | Purpose |
|-----|---------|
| `lint` | ktlint; all other jobs depend on it |
| `api-tests` | `./gradlew apiTest`, uploads Allure results |
| `ui-tests` | `./gradlew uiTest` with Playwright (chromium) |
| `allure-report` | merges results + history, publishes to [GitHub Pages](https://obrams.github.io/qa-kotlin-framework/) |
| `docker-build` | builds the image on `main` |

Manual runs: **Actions → Run workflow** with a `test_filter` input.

## Configuration

| Variable | Purpose | Default |
|----------|---------|---------|
| `CI` | headless browser mode | `true` in CI |
| `PETSTORE_URL` | API base URL | `https://petstore.swagger.io/v2` |

## Roadmap

- Replace the public demo targets with a self-contained `docker-compose` stack (REST + gRPC + Kafka) so the suite runs fully offline
- Contract tests: Schemathesis against the OpenAPI spec, Pact for consumer-driven contracts
- Testcontainers for the stack above
- OpenTelemetry traces per test for correlation with service logs

## License

[MIT](LICENSE)
