# ========================
# Стадия 1: сборка JAR с кэшем зависимостей
# ========================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Параметры JVM для Maven
ENV JAVA_OPTS="-Xms2g -Xmx6g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -XX:+ParallelRefProcEnabled"

WORKDIR /app

# 1. Копируем только pom.xml и файлы Maven Wrapper (если есть)
COPY pom.xml .
COPY .mvn/ .mvn
COPY mvnw .

# 2. Кэшируем зависимости — этот слой изменится только если изменился pom.xml
RUN ./mvnw dependency:go-offline

# 3. Копируем исходники
COPY src ./src

# 4. Собираем проект (без тестов для ускорения)
RUN ./mvnw clean package -DskipTests

# ========================
# Стадия 2: запуск приложения
# ========================
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Копируем собранный JAR из builder-слоя
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

