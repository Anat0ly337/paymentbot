package com.example.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class EchoBot extends TelegramLongPollingBot {

    private Set<Integer> summ = new HashSet<>();
    @Autowired
    private PaymentService bt;

    @Value("${bot.username}")
    private String username;

    @Value("${bot.token}")
    private String token;

    private static final Logger log = LoggerFactory.getLogger(EchoBot.class);

    @PostConstruct
    public void start() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            log.info("EchoBot started as @{}", getBotUsername());
        } catch (Exception e) {
            log.error("Failed to register bot", e);
        }
    }

    @Override
    public void onRegister() {
        try {
            execute(new SetMyCommands(
                    Arrays.asList(
                            new BotCommand("start", "Запустить бота"),
                            new BotCommand("create", "Создать платеж")
                    ),
                    new BotCommandScopeDefault(),
                    null
            ));
        } catch (TelegramApiException e) {
            log.error(e.getMessage() + LocalDateTime.now());
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            String reply;
            if ("/start".equals(text)) {
                reply = "Бот для создание платежей \n" +
                        "Введите команду /create чтобы создать платежную сессию \n" +
                        "Позже скопируйте ссылку и отдайте клиенту";
            } else if ("/create".equals(text)) {
                reply = "Введите сумму в рублях \n" +
                        "Минимальная сумма 1000 RUB";
            }else {
                reply = "<pre><code>" + createLink(text, chatId) + "</code></pre>";


                SendMessage msg = SendMessage.builder()
                        .chatId(chatId)
                        .text(reply)
                        .parseMode(ParseMode.HTML)        // важно!
                        .disableWebPagePreview(true)
                        .build();

                try {
                    execute(msg);
                } catch (TelegramApiException e) {
                    log.error("Failed to send message", e);
                }

                return;
            }

            SendMessage msg = SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(reply)
                    .build();

            try {
                execute(msg);
            } catch (TelegramApiException e) {
                log.error("Failed to send message", e);
            }
        }
    }


    private String createLink(String input, long chatId) {
        Integer value;
        String redirectUrl;
        try {

            String orderId = "ORDER-" + UUID.randomUUID();

            // готовим дополнительные поля (необязательно, но полезно)
            Map<String, String> extra = Map.of(
                    "description", "Test order " + orderId,
                    "urlSuccess", "https://paymentbot-production.up.railway.app/success?tgId=" + String.valueOf(chatId),
                    "urlFail",    "https://paymentbot-production.up.railway.appfail?tgId=" + String.valueOf(chatId),
                    "callbackUrl","https://paymentbot-production.up.railway.app/callback?tgId=" + String.valueOf(chatId),
                    "fullCallback","1"
            );
             value = Integer.parseInt(input);

            String respBody = bt.payment(String.valueOf(value), "RUB", orderId, extra);

            // пытаемся вытащить ссылку на оплату из JSON
             redirectUrl = bt.extractRedirectUrl(respBody);
            if (!StringUtils.hasText(redirectUrl)) {
                return  "Не удалось получить redirect_url из ответа: " + respBody;
            }
        } catch (NumberFormatException e) {
            return "Неверный формат суммы";
        } catch (IOException| InterruptedException e) {
            log.error(e.getMessage());
            return "Ошибка на стороне платежного шлюза";
        }
        return redirectUrl;
    }
}
