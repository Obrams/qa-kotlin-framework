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

### Сборка образа

```bash
docker build -t qa-kotlin-framework .
```

### Запуск тестов

```bash
# Все тесты
docker run --rm -v $(pwd)/build:/app/build qa-kotlin-framework

# API тесты
docker run --rm -v $(pwd)/build:/app/build qa-kotlin-framework test --tests "*Api*"

# UI тесты
docker run --rm -v $(pwd)/build:/app/build qa-kotlin-framework test --tests "*ui*"
```

### Docker Compose

```bash
# Запуск тестов
docker-compose up tests

# Запуск Allure сервера (http://localhost:5252)
docker-compose up -d allure allure-ui

# Всё вместе
docker-compose up tests allure allure-ui
```

## CI/CD

Проект использует **GitHub Actions** для автоматизации:

| Workflow | Описание |
|----------|----------|
| `lint` | Проверка кода с ktlint |
| `api-tests` | Запуск API тестов |
| `ui-tests` | Запуск UI тестов (matrix: chromium) |
| `allure-report` | Генерация и публикация отчёта |
| `docker-build` | Сборка Docker образа |

### Ручной запуск

Можно запустить тесты вручную через **Actions → Run workflow** с параметрами:
- `test_filter` — фильтр тестов (например, `*Api*`)
- `browser` — браузер для UI тестов

### Allure Report

Отчёты публикуются на **GitHub Pages** после merge в main/master:
```
https://<username>.github.io/<repo-name>/
```

## License

MIT
