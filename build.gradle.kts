plugins {
	java
	id("org.springframework.boot") version "3.5.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.ddbs"
version = "0.0.1-SNAPSHOT"
description = "Reminder service for session notifications"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(24)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java")
        }
    }
}

dependencies {
    // Core Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // For making HTTP requests to other microservices
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // Database - using pure JDBC like auth service
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    
    // Scheduling
    implementation("org.springframework.boot:spring-boot-starter")
    
    // Lombok for reducing boilerplate
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}