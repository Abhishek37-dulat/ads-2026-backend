package com.relay.connection;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionRepository extends JpaRepository<Connection, UUID> {
    // RLS scopes results to the current workspace; no explicit workspace filter needed.
    List<Connection> findAllByOrderByPlatformAsc();
}
