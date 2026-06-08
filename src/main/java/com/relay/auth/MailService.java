package com.relay.auth;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Transactional email via SMTP. If no mail sender is configured (SMTP_HOST blank) it degrades to a
 * no-op that reports "not sent", so the app still runs in dev — callers then surface a dev link.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final String from;

    public MailService(ObjectProvider<JavaMailSender> senderProvider,
                       @Value("${relay.mail.from:no-reply@relay.dev}") String from) {
        this.senderProvider = senderProvider;
        this.from = from;
    }

    public boolean enabled() {
        return senderProvider.getIfAvailable() != null;
    }

    /** Send an HTML email. Returns true if dispatched. */
    public boolean send(String to, String subject, String html) {
        JavaMailSender sender = senderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("[mail] SMTP not configured — skipping email to {} ({})", to, subject);
            return false;
        }
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
            h.setFrom(from);
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true);
            sender.send(msg);
            log.info("[mail] sent '{}' to {}", subject, to);
            return true;
        } catch (Exception e) {
            log.error("[mail] failed to send to {}: {}", to, e.getMessage());
            return false;
        }
    }

    public boolean sendVerification(String to, String name, String link) {
        String html = """
            <div style="font-family:system-ui,sans-serif;max-width:480px;margin:auto">
              <h2 style="color:#17161B">Verify your email</h2>
              <p>Hi %s, welcome to Relay. Confirm your email to activate your account.</p>
              <p style="margin:24px 0">
                <a href="%s" style="background:#4B3FE4;color:#fff;padding:12px 22px;border-radius:10px;text-decoration:none;font-weight:600">Verify email</a>
              </p>
              <p style="color:#6b6873;font-size:13px">Or paste this link: <br>%s</p>
              <p style="color:#9a97a2;font-size:12px">This link expires in 24 hours.</p>
            </div>""".formatted(escape(name), link, link);
        return send(to, "Verify your Relay account", html);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
