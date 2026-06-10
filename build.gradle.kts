import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.relay"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["springModulithVersion"] = "2.0.0"
extra["temporalVersion"] = "1.35.0"
extra["testcontainersVersion"] = "1.21.3"

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

dependencies {
    // --- web / api
    implementation("org.springframework.boot:spring-boot-starter-web")
    // Boot 4 modularized Jackson auto-config; this provides the ObjectMapper bean.
    implementation("org.springframework.boot:spring-boot-starter-json")
    // Spring Boot 4 dropped spring-boot-starter-aop; spring-aop ships with spring-context,
    // and aspectjweaver (BOM-managed) provides @Aspect support.
    implementation("org.aspectj:aspectjweaver")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // --- security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // --- data
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // Spring Boot 4 modularized auto-configuration: Flyway's lives in spring-boot-flyway
    // (which pulls flyway-core). Without this module Flyway never runs.
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // --- messaging
    implementation("org.springframework.kafka:spring-kafka")

    // --- email (SMTP)
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // --- modulith
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    runtimeOnly("org.springframework.modulith:spring-modulith-actuator")

    // --- analytics (shaded all-in-one jar: relocates its deps, avoids the lz4/kafka conflict)
    implementation("com.clickhouse:clickhouse-jdbc:0.7.2:all") {
        isTransitive = false
    }

    // --- durable orchestration
    implementation("io.temporal:temporal-sdk:${property("temporalVersion")}")

    // --- auth (self-issued JWT)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // --- ops / docs
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")

    // --- dev
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // --- test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<BootJar> {
    archiveFileName.set("relay-backend.jar")
}
