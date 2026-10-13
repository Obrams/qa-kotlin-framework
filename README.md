# Test Automation Framework (Kotlin)

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.5-green.svg)](https://gradle.org)
[![JUnit5](https://img.shields.io/badge/JUnit-5.10-orange.svg)](https://junit.org/junit5)
[![Playwright](https://img.shields.io/badge/Playwright-1.40-purple.svg)](https://playwright.dev)

Test automation framework with API and UI testing capabilities.

## Structure

```
src/main/kotlin/
├── core/
│   ├── api/           # HTTP client
│   ├── ui/            # Page Object base
│   └── threading/     # Thread-safe utilities
├── data/
│   ├── models/        # DTOs
│   └── factories/     # Test data generators
└── utils/             # Extensions

src/test/kotlin/
├── api/               # Petstore API tests
├── ui/
│   ├── saucedemo/     # SauceDemo tests
│   └── practice/      # Practice TA tests
├── concurrent/        # Thread safety tests
└── integration/       # E2E tests
```

## Components

### TestContext

```kotlin
TestContext.initContext("testName", "userId")
TestContext.trackRequestId(requestId)
TestContext.cleanup()
```

### ApiClient

```kotlin
val response = apiClient.get<Pet>("/pet/1")
val createResponse = apiClient.post<Pet, Pet>("/pet", petData)

when (response) {
    is ApiResponse.Success -> response.data
    is ApiResponse.HttpError -> response.statusCode
    is ApiResponse.Error -> response.exception
}
```

### Page Objects

```kotlin
class LoginPage(page: Page) : BaseScreen(page) {
    private val usernameInput = css("#username")
    
    fun login(username: String, password: String): DashboardPage {
        usernameInput.clearAndType(username)
        return DashboardPage(page)
    }
}
```

## Quick Start

### Prerequisites
- JDK 17+
- Gradle 8.x

### Run Tests

```bash
./gradlew test

./gradlew test --tests "*" -Dgroups=api
./gradlew test --tests "*" -Dgroups=ui

./gradlew test --tests "com.automation.api.PetApiTests"
```

### Allure Report

```bash
./gradlew test allureReport
./gradlew allureServe
```

## Configuration

### Parallel Execution

`src/test/resources/junit-platform.properties`:

```properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `CI` | Run headless | `true` |
| `PETSTORE_URL` | API base URL | `https://petstore.swagger.io/v2` |

## Docker

### Build the image

```bash
docker build -t qa-kotlin-framework .
```

### Run tests

```bash
# All tests
docker run --rm -v $(pwd)/build:/app/build qa-kotlin-framework

# API tests
docker run --rm -v $(pwd)/build:/app/build qa-kotlin-framework test --tests "*Api*"

# UI tests
docker run --rm -v $(pwd)/build:/app/build qa-kotlin-framework test --tests "*ui*"
```

### Docker Compose

```bash
# Run tests
docker-compose up tests

# Start Allure server (http://localhost:5252)
docker-compose up -d allure allure-ui

# Everything together
docker-compose up tests allure allure-ui
```

## CI/CD

The project uses **GitHub Actions** for automation:

| Workflow | Description |
|----------|----------|
| `lint` | Code linting with ktlint |
| `api-tests` | Run API tests |
| `ui-tests` | Run UI tests (matrix: chromium) |
| `allure-report` | Generate and publish the report |
| `docker-build` | Build the Docker image |

### Manual run

Tests can be run manually via **Actions → Run workflow** with parameters:
- `test_filter` — test filter (e.g. `*Api*`)
- `browser` — browser for UI tests

### Allure Report

Reports are published to **GitHub Pages** after merge into main/master:
```
https://<username>.github.io/<repo-name>/
```

## License

MIT
