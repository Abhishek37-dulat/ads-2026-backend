package com.relay.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.relay.identity.AppUser;
import com.relay.identity.AppUserRepository;
import com.relay.identity.TenantProvisioningService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    @Test
    void newGoogleUsersReceiveDifferentProvisionedWorkspaces() {
        AppUserRepository users = mock(AppUserRepository.class);
        TenantProvisioningService tenants = mock(TenantProvisioningService.class);
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        AtomicInteger sequence = new AtomicInteger();
        when(tenants.createUser(anyString(), anyString(), isNull(), anyBoolean(), anyBoolean(), anyString()))
            .thenAnswer(invocation -> user(invocation.getArgument(0), sequence.incrementAndGet()));
        when(tenants.workspaceFor(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            return new TenantProvisioningService.WorkspaceIdentity(
                user.getDefaultWorkspaceId(), user.getName() + " Workspace");
        });

        AuthService service = service(users, tenants, mock(SmsService.class), true, false);
        AuthService.AuthResult first = service.googleLogin("one@example.com", "One");
        AuthService.AuthResult second = service.googleLogin("two@example.com", "Two");

        assertNotEquals(first.workspaceId(), second.workspaceId());
    }

    @Test
    void productionOtpDeliveryFailureDoesNotExposeCode() {
        SmsService sms = mock(SmsService.class);
        when(sms.sendOtp(anyString(), anyString())).thenReturn(false);
        AuthService service = service(mock(AppUserRepository.class),
            mock(TenantProvisioningService.class), sms, false, true);

        assertThrows(AuthService.DeliveryUnavailableException.class,
            () -> service.startOtp("+919999999999"));
    }

    private static AuthService service(AppUserRepository users, TenantProvisioningService tenants,
                                       SmsService sms, boolean exposeDevSecrets,
                                       boolean requireDelivery) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        return new AuthService(
            users,
            mock(JwtService.class),
            redis,
            mock(PasswordEncoder.class),
            mock(MailService.class),
            sms,
            tenants,
            "http://localhost:8081",
            exposeDevSecrets,
            requireDelivery);
    }

    private static AppUser user(String email, int sequence) {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setOrgId(UUID.randomUUID());
        user.setDefaultWorkspaceId(new UUID(0, sequence));
        user.setEmail(email);
        user.setName(email);
        user.setEmailVerified(true);
        return user;
    }
}
