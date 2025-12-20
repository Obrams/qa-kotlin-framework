# ========================================
# Test Automation Framework - Docker Image
# ========================================
# Multi-stage build для оптимального размера

# Stage 1: Базовый образ с Playwright
FROM mcr.microsoft.com/playwright:v1.40.0-jammy AS base

# Устанавливаем JDK 21
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    && wget -O - https://apt.corretto.aws/corretto.key | gpg --dearmor -o /usr/share/keyrings/corretto-keyring.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/corretto-keyring.gpg] https://apt.corretto.aws stable main" | tee /etc/apt/sources.list.d/corretto.list \
    && apt-get update \
    && apt-get install -y java-21-amazon-corretto-jdk \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Stage 2: Кэширование зависимостей Gradle
FROM base AS dependencies

WORKDIR /app

# Копируем только файлы для разрешения зависимостей
COPY gradle/ gradle/
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Загружаем зависимости
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Stage 3: Финальный образ
FROM dependencies AS runtime

WORKDIR /app

# Копируем исходный код
COPY src/ src/

# Устанавливаем переменные окружения
ENV CI=true
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"

# Создаём директорию для отчётов
RUN mkdir -p /app/build/allure-results

# Точка входа по умолчанию - запуск всех тестов
ENTRYPOINT ["./gradlew"]
CMD ["test", "allureReport"]

# ========================================
# Примеры запуска:
# ========================================
# 
# Сборка образа:
#   docker build -t qa-kotlin-framework .
#
# Запуск всех тестов:
#   docker run --rm -v $(pwd)/build:/app/build qa-kotlin-framework
#
# Запуск только API тестов:
#   docker run --rm -v $(pwd)/build:/app/build qa-kotlin-framework test --tests "*Api*"
#
# Запуск только UI тестов:
#   docker run --rm -v $(pwd)/build:/app/build qa-kotlin-framework test --tests "*ui*"
#
# Интерактивный режим:
#   docker run --rm -it -v $(pwd)/build:/app/build --entrypoint /bin/bash qa-kotlin-framework


