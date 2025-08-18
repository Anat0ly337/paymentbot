package com.example.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PaymentService {
    // V1 базовый URL
    private static final String BASE_URL_V1 = "https://merchant.betatransfer.io";

    @Value("${betatransfer.publicKey}")
    private  String publicKey;

    @Value("${betatransfer.secretKey}")
    private  String secret;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper om = new ObjectMapper();

    /**
     * Создание платежа: POST form на /api/payment?token=PUBLIC, sign=md5(implode(values)+secret)
     */
    public String payment(String amount, String currency, String orderId, Map<String, String> extra) throws Exception {
        // порядок важен → LinkedHashMap
        Map<String, String> form = new LinkedHashMap<>();
        form.put("amount", amount);
        form.put("currency", currency);
        form.put("orderId", orderId);
        if (extra != null) form.putAll(extra);

        // подпись по образцу из PHP
        form.put("sign", md5Hex(implodeValues(form) + secret));

        String url = BASE_URL_V1 + "/api/payment?token=" + enc(publicKey);
        String body = toForm(form);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }

    /**
     * Пытаемся вытащить ссылку на оплату из JSON-ответа
     */
    public String extractRedirectUrl(String json) {
        try {
            JsonNode n = om.readTree(json);
            // в разных инстансах поле может называться по-разному
            for (String key : new String[]{"redirect_url", "url", "payment_url", "checkout_url", "redirectUrl"}) {
                if (n.hasNonNull(key)) return n.get(key).asText();
            }
            // иногда ссылка лежит глубже
            if (n.has("data") && n.get("data").isObject()) {
                JsonNode d = n.get("data");
                for (String key : new String[]{"redirect_url", "url", "payment_url", "checkout_url"}) {
                    if (d.hasNonNull(key)) return d.get(key).asText();
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /* ===== утилиты ===== */

    private static String md5Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String toForm(Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        for (var e : form.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(enc(e.getKey())).append('=').append(enc(e.getValue() == null ? "" : e.getValue()));
        }
        return sb.toString();
    }

    private static String implodeValues(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (String v : map.values()) sb.append(v == null ? "" : v);
        return sb.toString();
    }
}
