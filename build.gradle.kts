import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val dqlangVersion = "2.0.16"

plugins {
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.8.20"
    kotlin("plugin.spring") version "1.8.20"

    id("net.researchgate.release") version "3.0.2"
    id("maven-publish")
    id("idea")
    id("eclipse")
    id("com.diffplug.spotless") version "6.24.0"
}

spotless {
    java {
        googleJavaFormat()
        formatAnnotations()
        removeUnusedImports()
    }
    kotlin {
        ktlint()
            .editorConfigOverride(
                mapOf(
                    "indent_size" to 2,
                    "continuation_indent_size" to "2",
                ),
            )
        ktfmt().googleStyle()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

idea { // allows downloading sources and javadoc for IntelliJ with gradle cleanIdea idea
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

eclipse { // allows downloading sources and javadoc for Eclipse with gradle cleanEclipse eclipse
    classpath {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

group = "dqualizer"

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

release {
    // no config needed, see https://github.com/researchgate/gradle-release for options
}

publishing {
    repositories {
        maven {
            name = "gpr"
            url = uri("https://maven.pkg.github.com/dqualizer/dqexec")
            credentials(PasswordCredentials::class)
        }
        publications {
            register("jar", MavenPublication::class) {
                from(components["java"])
                pom {
                    url.set("https://github.com/dqualizer/dqexec.git")
                }
            }
        }
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven {
        name = "gpr"
        url = uri("https://maven.pkg.github.com/dqualizer/dqlang")
        credentials(PasswordCredentials::class)
    }
}

extra["testcontainersVersion"] = "1.17.6"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("io.micrometer:micrometer-tracing-bridge-brave:1.0.3")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave:2.16.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")
    implementation("org.mapstruct:mapstruct:1.5.3.Final")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.freemarker:freemarker:2.3.31")
    implementation("org.springframework.vault:spring-vault-core:3.0.2")
    implementation("io.github.dqualizer:dqlang:$dqlangVersion")
    compileOnly("org.projectlombok:lombok:1.18.26")
    runtimeOnly("com.h2database:h2:2.1.214")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.amqp:spring-rabbit-test:3.0.2")
    testImplementation("org.testcontainers:junit-jupiter:1.17.6")
    testImplementation("org.testcontainers:rabbitmq:1.18.0")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Disable generation of "-plain" jar by the Spring Boot plugin
tasks.getByName<Jar>("jar") {
    enabled = false
}
