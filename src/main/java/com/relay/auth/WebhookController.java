package com.relay.auth;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public webhook receivers for Fast2SMS. Enter these URLs (on your public domain) in the Fast2SMS
 * console:
 *   SMS delivery reports → https://&lt;domain&gt;/api/sms/dlr
 *   WhatsApp events      → https://&lt;domain&gt;/api/whatsapp/webhook
 * Both are optional — outbound OTP works without them; these carry delivery receipts / inbound.
 */
@RestController
@RequestMapping("/api")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @PostMapping("/sms/dlr")
    public Map<String, Object> smsDlr(@RequestBody(required = false) Map<String, Object> body) {
        log.info("[fast2sms-dlr] {}", body);
        return Map.of("ok", true);
    }

    @PostMapping("/whatsapp/webhook")
    public Map<String, Object> whatsapp(@RequestBody(required = false) Map<String, Object> body) {
        log.info("[fast2sms-whatsapp] {}", body);
        return Map.of("ok", true);
    }
}
