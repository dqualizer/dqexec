import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val dqlangVersion = "3.1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.spring") version "1.9.0"

    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"

    id("net.researchgate.release") version "3.0.2"
    id("maven-publish")
    id("idea")
    id("eclipse")
}

idea { //allows downloading sources and javadoc for IntelliJ with gradle cleanIdea idea
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

eclipse {
    classpath {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

group = "dqualizer"

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

release {
    //no config needed, see https://github.com/researchgate/gradle-release for options
}

publishing {
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
    mavenLocal()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://jitpack.io") }
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
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    implementation("org.springframework.vault:spring-vault-core:3.1.1")
    implementation("org.springframework.plugin:spring-plugin-core:3.0.0")

    implementation("com.github.docker-java:docker-java:3.3.4") {
        exclude(group = "org.slf4j")
    }

    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.4")
    implementation("org.apache.httpcomponents.core5:httpcore5-h2:5.2.4") //dependency of docker-java

    implementation("com.squareup.okhttp3:okhttp:4.12.0")


    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.3")

    implementation(files("inspectit-ocelot-config-2.6.4.jar"))
//    implementation("rocks.inspectit.ocelot:inspectit-ocelot-config:SNAPSHOT")

    implementation("io.github.oshai:kotlin-logging:6.0.3")

    implementation("io.opentelemetry:opentelemetry-api:1.34.1")


    implementation("io.github.dqualizer:dqlang:${dqlangVersion}")
//    implementation(project(":dqlang"))

    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.amqp:spring-rabbit-test")
    testImplementation("org.testcontainers:junit-jupiter:1.17.6")
    testImplementation("org.testcontainers:rabbitmq:1.18.0")
    testImplementation("org.jeasy:easy-random-core:5.0.0")
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

//allows to use parameter names in reflection (needed for mongoDB repositories)
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
