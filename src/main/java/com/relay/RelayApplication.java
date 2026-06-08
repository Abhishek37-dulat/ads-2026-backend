package com.relay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Relay — multi-platform ad orchestration.
 *
 * <p>One canonical campaign brief is validated, made compliant, and fanned out to every ad
 * platform in parallel. Module boundaries (identity, campaign, adapters, orchestration, …) are
 * enforced with Spring Modulith package conventions under {@code com.relay}.
 */
@Modulithic(systemName = "Relay")
@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement(order = 0)
public class RelayApplication {
    public static void main(String[] args) {
        SpringApplication.run(RelayApplication.class, args);
    }
}
