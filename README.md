# Test Automation Framework (Kotlin)

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.5-green.svg)](https://gradle.org)
[![JUnit5](https://img.shields.io/badge/JUnit-5.10-orange.svg)](https://junit.org/junit5)
[![Playwright](https://img.shields.io/badge/Playwright-1.40-purple.svg)](https://playwright.dev)
[![Allure Report](https://img.shields.io/badge/Allure-Report-orange)](https://obrams.github.io/qa-kotlin-framework/)
[![CI](https://github.com/Obrams/qa-kotlin-framework/actions/workflows/tests.yml/badge.svg?branch=main)](https://github.com/Obrams/qa-kotlin-framework/actions/workflows/tests.yml)

Test automation framework with API and UI testing capabilities.

**📊 [View Latest Allure Report](https://obrams.github.io/qa-kotlin-framework/)**

**About:** Kotlin-based concurrent-safe test automation framework with unified API and UI testing capabilities, ready for Docker deployment.

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

## Design

### TestContext

`TestContext` реализован на базе `ThreadLocal` для thread-safe изоляции данных каждого теста. Решает проблемы:

- **Логирование**: Автоматическое логирование с контекстом теста (имя, thread ID, метаданные)
- **Trace**: Отслеживание request ID для всех HTTP-запросов в рамках теста, что позволяет связать логи с конкретными запросами
- **Multi-thread**: Полная изоляция контекста между параллельно выполняемыми тестами, исключающая race conditions

Каждый тест инициализирует свой контекст через `initContext()`, который хранит тестовые данные, метаданные и список запросов. При параллельном выполнении каждый поток имеет независимый экземпляр контекста.

### ApiClient и ApiResponse

`ApiClient` построен на Spring WebClient с реактивной поддержкой. Использует sealed class `ApiResponse<T>` с тремя вариантами:

- **`ApiResponse.Success<T>`**: Успешный ответ с десериализованными данными, статус-кодом и заголовками
- **`ApiResponse.HttpError`**: HTTP ошибки (4xx/5xx) с кодом статуса, сообщением и телом ответа
- **`ApiResponse.Error`**: Сетевые ошибки и исключения с детальной информацией

Все запросы автоматически логируются в `ThreadSafeReportCollector`, который собирает метрики (длительность, статус-коды, ошибки) в thread-safe режиме. Поддерживаются retry для временных ошибок (408, 429, 5xx) и таймауты.

### Page Objects

Базовая реализация через `BaseScreen` с набором удобных хелперов для работы с элементами:

- **Локаторы**: `css()`, `testId()`, `text()`, `role()`, `xpath()` — DSL для удобного описания элементов
- **`ScreenElement`**: Обёртка над Playwright `Locator` с методами `click()`, `type()`, `clearAndType()`, автоматическим ожиданием видимости
- **Навигация**: Встроенные методы `navigate()`, `waitForLoad()`, `waitForNavigation()`
- **Скриншоты**: Автоматическое создание скриншотов с привязкой к Allure

Page Objects возвращают следующий экран, что обеспечивает fluent API и читаемость тестов.

## Examples

### API Test Example

```kotlin
@Test
@DisplayName("Should create, update and delete pet with error handling")
fun testPetLifecycle() {
    val pet = TestDataFactory.createPet(
        name = "Fluffy",
        status = PetStatus.AVAILABLE
    )

    val createResponse = apiClient.post<Pet, Pet>("/pet", pet)
    
    when (createResponse) {
        is ApiResponse.Success -> {
            val createdPet = createResponse.data
            assertThat(createdPet.id).isNotNull()
            assertThat(createdPet.name).isEqualTo("Fluffy")
            
            val updatedPet = createdPet.copy(
                name = "Fluffy Updated",
                status = PetStatus.SOLD
            )
            val updateResponse = apiClient.put<Pet, Pet>("/pet", updatedPet)
            
            assertThat(updateResponse)
                .isInstanceOf(ApiResponse.Success::class.java)
            assertThat((updateResponse as ApiResponse.Success).data.status)
                .isEqualTo(PetStatus.SOLD)
        }
        is ApiResponse.HttpError -> {
            fail("Failed to create pet: ${createResponse.statusCode} - ${createResponse.message}")
        }
        is ApiResponse.Error -> {
            fail("Unexpected error: ${createResponse.message}", createResponse.exception)
        }
    }
}

@Test
@DisplayName("Should handle 404 for non-existent pet")
fun testGetNonExistentPet() {
    val response = apiClient.get<Pet>("/pet/999999999999")
    
    assertThat(response).isInstanceOf(ApiResponse.HttpError::class.java)
    val error = response as ApiResponse.HttpError
    assertThat(error.statusCode).isEqualTo(404)
    assertThat(error.body).isNotNull()
}
```

### UI Test Example

```kotlin
@Test
@DisplayName("Should complete checkout flow successfully")
fun testCompleteCheckout() = withScreenshotOnFailure {
    val inventoryPage = InventoryPage(page)
    
    inventoryPage.addToCart("Sauce Labs Backpack")
    inventoryPage.addToCart("Sauce Labs Bike Light")
    
    assertThat(inventoryPage.getCartItemCount()).isEqualTo(2)
    
    val cartPage = inventoryPage.openCart()
    assertThat(cartPage.getCartItemNames())
        .containsExactly("Sauce Labs Backpack", "Sauce Labs Bike Light")
    
    val checkoutPage = cartPage.checkout()
    checkoutPage
        .fillInfo("John", "Doe", "12345")
        .continueCheckout()
    
    assertThat(checkoutPage.getTotalPrice()).isNotEmpty()
    assertThat(checkoutPage.getSubtotal()).isNotEmpty()
    
    checkoutPage.finish()
    
    assertThat(checkoutPage.isCheckoutComplete()).isTrue()
    assertThat(checkoutPage.getCompletionMessage())
        .containsIgnoringCase("thank you for your order")
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

Проект использует **GitHub Actions** для автоматизации тестирования и публикации отчётов.

### Pipeline Schema

```
┌─────────┐
│  Push   │
│  / PR   │
└────┬────┘
     │
     ▼
┌─────────────┐
│   🔍 Lint   │ ◄─── Проверка кода (ktlint)
└─────┬───────┘
      │
      ├─────────────────┐
      │                 │
      ▼                 ▼
┌─────────────┐  ┌─────────────┐
│ API Tests   │  │  UI Tests   │ ◄─── Matrix: chromium
└─────┬───────┘  └──────┬──────┘
      │                 │
      │                 │
      └────────┬────────┘
               │
               ▼
      ┌─────────────────┐
      │  Allure Report   │ ◄─── Генерация и публикация
      └─────────────────┘
               │
               ├──► GitHub Pages (gh-pages)
               ├──► Allure History (артефакт)
               ├──► Playwright Videos (артефакт)
               └──► Playwright Screenshots (артефакт)
```

### Workflows

#### 🧪 Test Automation (`tests.yml`)

Основной workflow, запускается при push/PR в `main`, `master`, `develop` или вручную.

**Jobs:**

| Job | Описание | Зависимости | Артефакты |
|-----|----------|-------------|-----------|
| `lint` | Проверка кода с ktlint | - | - |
| `api-tests` | Запуск API тестов | `lint` | `allure-results-api`, `test-results-api` |
| `ui-tests` | Запуск UI тестов (matrix: chromium) | `lint` | `allure-results-ui-*`, `test-results-ui-*`, `playwright-videos-*`, `playwright-screenshots-*` |
| `full-test-suite` | Полный прогон (только ручной запуск) | - | `allure-results-full` |
| `allure-report` | Генерация и публикация Allure отчёта | `api-tests`, `ui-tests` | `allure-report`, `allure-history`, `playwright-videos-all`, `playwright-screenshots-all` |
| `docker-build` | Сборка Docker образа | `lint` | - |

**Артефакты доступны для скачивания:**
- 📊 **Allure Report** — полный отчёт с историей
- 📚 **Allure History** — история запусков для трендов
- 🎬 **Playwright Videos** — видео выполнения UI тестов
- 📸 **Playwright Screenshots** — скриншоты из UI тестов
- 📋 **Test Results** — JUnit XML результаты

**Ручной запуск:**

Через **Actions → Run workflow** с параметрами:
- `test_filter` — фильтр тестов (например, `*Api*`, `*ui*`)
- `browser` — браузер для UI тестов (`chromium`, `firefox`, `webkit`)

#### 📄 Publish Allure Report (`allure-pages.yml`)

Отдельный workflow для публикации Allure отчёта на GitHub Pages. Запускается при push в `main`/`master` или вручную.

### Allure Report

Отчёты публикуются на **GitHub Pages** после merge в main/master:
- 🔗 [Latest Allure Report](https://obrams.github.io/qa-kotlin-framework/)
- 📦 Артефакты доступны в каждом workflow run в разделе **Artifacts**

## License

MIT
