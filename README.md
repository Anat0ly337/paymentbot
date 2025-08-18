# Spring Boot 3 + Telegram Echo Bot

Простой пример бота на Spring Boot 3, который принимает входящие сообщения и отвечает эхом.

## Как запустить

1) Создайте бота у @BotFather и получите **TOKEN** и **USERNAME**.

2) Установите Java 17+ и Maven.

3) В корне проекта создайте файл `src/main/resources/application.yml` или используйте переменные окружения:

```bash
export TELEGRAM_BOT_TOKEN=123456:ABC...
export TELEGRAM_BOT_USERNAME=my_echo_bot
mvn spring-boot:run
```

Либо:
```bash
mvn -DTELEGRAM_BOT_TOKEN=... -DTELEGRAM_BOT_USERNAME=... spring-boot:run
```

4) Напишите боту в Telegram. На любое текстовое сообщение он отвечает эхом.
Команда `/start` выдаёт приветствие.

## Структура
- `TelegramEchoApplication` — точка входа Spring Boot
- `BotProperties` — конфиг для токена/имени
- `EchoBot` — реализация `TelegramLongPollingBot` (получает апдейты и шлёт ответы)
