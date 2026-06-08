package com.relay.connection;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCheckRepository extends JpaRepository<VerificationCheck, UUID> {
    List<VerificationCheck> findByConnectionId(UUID connectionId);
}
