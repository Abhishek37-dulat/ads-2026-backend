package com.relay.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Ensures a platform admin account exists on startup (credentials from env). */
@Component
public class AdminBootstrap {

    private final AuthService auth;
    private final String email;
    private final String password;

    public AdminBootstrap(AuthService auth,
                          @Value("${relay.admin.email:admin@relay.dev}") String email,
                          @Value("${relay.admin.password:admin12345}") String password) {
        this.auth = auth;
        this.email = email;
        this.password = password;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedAdmin() {
        auth.ensureAdmin(email, password, "Platform Admin");
    }
}
