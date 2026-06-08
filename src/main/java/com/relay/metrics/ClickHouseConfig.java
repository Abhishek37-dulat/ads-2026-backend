package com.relay.metrics;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ClickHouse analytics JdbcTemplate. The Hikari pool is built <em>inside</em> the bean method and
 * not exposed as a {@code DataSource} bean — exposing a second DataSource would make Spring Boot
 * back off auto-configuring the primary Postgres datasource that JPA/Flyway rely on.
 */
@Configuration
public class ClickHouseConfig {

    @Bean
    JdbcTemplate clickHouseJdbc(
        @Value("${relay.clickhouse.url}") String url,
        @Value("${relay.clickhouse.user}") String user,
        @Value("${relay.clickhouse.password}") String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        cfg.setMaximumPoolSize(4);
        cfg.setPoolName("clickhouse");
        return new JdbcTemplate(new HikariDataSource(cfg));
    }
}
