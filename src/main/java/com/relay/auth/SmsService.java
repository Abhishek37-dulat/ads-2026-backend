package com.relay.auth;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * SMS OTP via Fast2SMS (bulkV2 "otp" route). If no API key is configured it degrades to a no-op so
 * the OTP flow still works in dev (the code is then surfaced in the response as a dev code).
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final String ENDPOINT = "https://www.fast2sms.com/dev/bulkV2";

    private final String apiKey;
    private final RestClient http = RestClient.create();

    public SmsService(@Value("${relay.sms.fast2sms-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean enabled() {
        return StringUtils.hasText(apiKey);
    }

    /** Send a numeric OTP to a phone via Fast2SMS. Returns true if dispatched. */
    public boolean sendOtp(String phone, String code) {
        if (!enabled()) {
            log.warn("[sms] Fast2SMS not configured — skipping OTP to {}", phone);
            return false;
        }
        String number = tenDigits(phone);
        if (number == null) {
            log.warn("[sms] non-10-digit number {} — cannot send via Fast2SMS", phone);
            return false;
        }
        try {
            String url = ENDPOINT + "?route=otp&variables_values=" + code + "&flash=0&numbers=" + number;
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = http.get().uri(url)
                .header("authorization", apiKey)
                .retrieve().body(Map.class);
            boolean ok = resp != null && Boolean.TRUE.equals(resp.get("return"));
            if (ok) {
                log.info("[sms] OTP sent to {} (request_id={})", number, resp.get("request_id"));
            } else {
                log.warn("[sms] Fast2SMS rejected OTP to {}: {}", number, resp);
            }
            return ok;
        } catch (Exception e) {
            log.error("[sms] Fast2SMS send failed for {}: {}", number, e.getMessage());
            return false;
        }
    }

    /** Fast2SMS expects a bare 10-digit Indian mobile; take the trailing 10 digits. */
    private static String tenDigits(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 10) return null;
        return digits.substring(digits.length() - 10);
    }
}
