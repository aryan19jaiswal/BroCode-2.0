plugins {
	java
	id("org.springframework.boot") version "3.5.10"
	id("io.spring.dependency-management") version "1.1.7"
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation ("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation ("org.springframework.boot:spring-boot-starter-security")
	implementation ("org.springframework.boot:spring-boot-starter-web")
	implementation ("org.springframework.boot:spring-boot-starter-validation")
	implementation ("org.springframework.boot:spring-boot-starter-webflux")

	// Redis — session store for horizontal scaling (replaces in-memory map in non-local profiles)
	implementation ("org.springframework.boot:spring-boot-starter-data-redis")

	// Guava — global Gemini API rate limiter (executed on boundedElastic, not Reactor threads)
	implementation ("com.google.guava:guava:33.0.0-jre")

	// Bucket4j — per-IP auth rate limiting + per-user chat rate limiting
	implementation ("com.bucket4j:bucket4j-core:8.10.1")

	// Actuator — health and info endpoints for Railway / Docker health checks
	implementation ("org.springframework.boot:spring-boot-starter-actuator")

	compileOnly ("org.projectlombok:lombok")
	annotationProcessor ("org.projectlombok:lombok")
	testImplementation ("org.springframework.boot:spring-boot-starter-test")
	testImplementation ("org.springframework.security:spring-security-test")

	// Embedded MongoDB for integration tests — auto-configures alongside @SpringBootTest
	testImplementation ("de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring3x:4.14.0")

	// JWT for Authentication
	implementation(platform("io.jsonwebtoken:jjwt-bom:0.13.0"))
	implementation("io.jsonwebtoken:jjwt-api")
	runtimeOnly("io.jsonwebtoken:jjwt-impl")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson")

	// Langchain4j
	implementation(platform("dev.langchain4j:langchain4j-bom:1.11.0"))
	implementation("dev.langchain4j:langchain4j")
	implementation("dev.langchain4j:langchain4j-google-ai-gemini")
	implementation("dev.langchain4j:langchain4j-core")
	implementation("dev.langchain4j:langchain4j-reactor")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
