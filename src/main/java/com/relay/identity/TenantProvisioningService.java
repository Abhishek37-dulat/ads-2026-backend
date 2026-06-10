package com.relay.identity;

import com.relay.shared.WorkspaceContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates and resolves the isolated organization/workspace owned by an identity. */
@Service
public class TenantProvisioningService {

    private final AppUserRepository users;

    @PersistenceContext
    private EntityManager em;

    public TenantProvisioningService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional
    public AppUser createUser(String email, String name, String passwordHash,
                              boolean emailVerified, boolean admin, String workspaceName) {
        UUID orgId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String displayName = name == null || name.isBlank() ? email.split("@")[0] : name.trim();

        em.createNativeQuery("INSERT INTO organization (id, name) VALUES (:id, :name)")
            .setParameter("id", orgId)
            .setParameter("name", admin ? "Relay Operations" : displayName + " Organization")
            .executeUpdate();

        withWorkspace(workspaceId);
        em.createNativeQuery(
                "INSERT INTO workspace (id, org_id, name) VALUES (:id, :org, :name)")
            .setParameter("id", workspaceId)
            .setParameter("org", orgId)
            .setParameter("name", workspaceName)
            .executeUpdate();

        AppUser user = new AppUser();
        user.setId(userId);
        user.setOrgId(orgId);
        user.setDefaultWorkspaceId(workspaceId);
        user.setEmail(email);
        user.setName(displayName);
        user.setPasswordHash(passwordHash);
        user.setEmailVerified(emailVerified);
        user.setAdmin(admin);
        user.setCreatedAt(OffsetDateTime.now());
        users.saveAndFlush(user);

        em.createNativeQuery(
                "INSERT INTO membership (id, workspace_id, user_id, role) "
                    + "VALUES (:id, :workspace, :user, 'admin')")
            .setParameter("id", UUID.randomUUID())
            .setParameter("workspace", workspaceId)
            .setParameter("user", userId)
            .executeUpdate();
        return user;
    }

    @Transactional(readOnly = true)
    public WorkspaceIdentity workspaceFor(AppUser user) {
        UUID workspaceId = user.getDefaultWorkspaceId();
        if (workspaceId == null) {
            throw new IllegalStateException("User has no default workspace");
        }
        withWorkspace(workspaceId);
        Object name = em.createNativeQuery("SELECT name FROM workspace WHERE id = :id")
            .setParameter("id", workspaceId)
            .getSingleResult();
        return new WorkspaceIdentity(workspaceId, name.toString());
    }

    private void withWorkspace(UUID workspaceId) {
        UUID previous = WorkspaceContext.get();
        WorkspaceContext.set(workspaceId);
        try {
            em.createNativeQuery("SELECT set_config('app.workspace', :ws, true)")
                .setParameter("ws", workspaceId.toString())
                .getSingleResult();
        } finally {
            if (previous == null) {
                WorkspaceContext.clear();
            } else {
                WorkspaceContext.set(previous);
            }
        }
    }

    public record WorkspaceIdentity(UUID id, String name) {}
}
