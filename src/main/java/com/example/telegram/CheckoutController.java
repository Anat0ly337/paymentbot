package com.example.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;
import java.util.UUID;

@RestController
public class CheckoutController {

    @Autowired
    private PaymentService bt;

    @Autowired
    private EchoBot echoBot;


    @GetMapping("/success")
    public ResponseEntity<?> success(@RequestParam("tgId") String tgId) throws Exception {
        SendMessage msg = SendMessage.builder()
                .chatId(tgId)
                .text("Успешная оплата")
                .build();
       echoBot.execute(msg);

        return null;
    }

    @GetMapping("/fail")
    public ResponseEntity<?> fail(@RequestParam("tgId") String tgId) throws Exception {
        SendMessage msg = SendMessage.builder()
                .chatId(tgId)
                .text("Неуспешная оплата")
                .build();
        echoBot.execute(msg);

        return null;
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam("tgId") String tgId) throws Exception {

        SendMessage msg = SendMessage.builder()
                .chatId(tgId)
                .text("callback")
                .build();
        echoBot.execute(msg);

        return null;
    }
}
