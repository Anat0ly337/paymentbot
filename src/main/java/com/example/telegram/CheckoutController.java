package com.example.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
public class CheckoutController {

    @Autowired
    private PaymentService bt;

    @GetMapping("/pay")
    public ResponseEntity<Void> pay(
            @RequestParam("amount") Integer amount
    ) throws Exception {
        // генерим orderId как пример
        String orderId = "ORDER-" + UUID.randomUUID();

        // готовим дополнительные поля (необязательно, но полезно)
        Map<String, String> extra = Map.of(
                "description", "Test order " + orderId,
                "urlSuccess", "http://localhost:8080/success",
                "urlFail",    "http://localhost:8080/success",
                "callbackUrl","http://localhost:8080/success",
                "fullCallback","1"
        );

        // вызываем Betatransfer: создаём платёж
        String respBody = bt.payment(String.valueOf(amount), "USD", orderId, extra);

        // пытаемся вытащить ссылку на оплату из JSON
        String redirectUrl = bt.extractRedirectUrl(respBody);
        if (!StringUtils.hasText(redirectUrl)) {
            throw new IllegalStateException("Не удалось получить redirect_url из ответа: " + respBody);
        }

        // редиректим покупателя на checkout
        return org.springframework.http.ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
    }

    @GetMapping("/success")
    public ResponseEntity<Void> success() throws Exception {
       return null;
    }
}
