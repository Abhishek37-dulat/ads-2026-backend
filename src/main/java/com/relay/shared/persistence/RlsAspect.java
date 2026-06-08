package com.relay.shared.persistence;

import com.relay.shared.WorkspaceContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Sets {@code app.workspace} on the current transaction's connection so Postgres Row-Level
 * Security scopes every query to the request's workspace.
 *
 * <p>Ordered to run <em>inside</em> Spring's transaction advice (which is pinned to order 0 via
 * {@code @EnableTransactionManagement(order = 0)} on the application), so the {@code set_config}
 * call shares the transaction's connection. Transaction-local ({@code true}) so it resets on
 * commit/rollback and never leaks across pooled connections.
 */
@Aspect
@Component
@Order(100)
public class RlsAspect {

    @PersistenceContext
    private EntityManager em;

    @Around("@within(org.springframework.transaction.annotation.Transactional) "
            + "|| @annotation(org.springframework.transaction.annotation.Transactional)")
    public Object applyWorkspace(ProceedingJoinPoint pjp) throws Throwable {
        UUID workspaceId = WorkspaceContext.get();
        if (workspaceId != null) {
            em.createNativeQuery("SELECT set_config('app.workspace', :ws, true)")
                .setParameter("ws", workspaceId.toString())
                .getSingleResult();
        }
        return pjp.proceed();
    }
}
