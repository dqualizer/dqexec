import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.1.0-M2"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.8.20"
    kotlin("plugin.spring") version "1.8.20"

    id("net.researchgate.release") version "3.0.2"
    id("maven-publish")
}

group = "dqualizer"

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

release {
    //no config needed, see https://github.com/researchgate/gradle-release for options
}

publishing{
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/dqualizer/dqexec")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
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
    maven { url = uri("https://repo.spring.io/milestone") }
}

extra["testcontainersVersion"] = "1.17.6"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("dqualizer:dqlang:0.2.0")

    compileOnly("org.projectlombok:lombok")
    runtimeOnly("com.h2database:h2")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.amqp:spring-rabbit-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:rabbitmq")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

tasks.withType<KotlinCompile> {
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
