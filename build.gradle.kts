val dqlangVersion = "4.0.8-SNAPSHOT"

plugins {
  kotlin("jvm") version "2.0.0"
  kotlin("plugin.spring") version "2.0.0"

  id("org.springframework.boot") version "3.2.3"
  id("io.spring.dependency-management") version "1.1.5"

  id("net.researchgate.release") version "3.0.2"
  id("maven-publish")
  id("idea")
  id("eclipse")

  id("com.adarshr.test-logger") version "4.0.0"
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

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

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
  maven { url = uri("https://repo.spring.io/milestone") }
  maven { url = uri("https://jitpack.io") }
  maven {
    name = "gpr"
    url = uri("https://maven.pkg.github.com/dqualizer/dqlang")
    credentials(PasswordCredentials::class)
  }
}

extra["testcontainersVersion"] = "1.19.7"

testlogger {
  showStackTraces = false
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
  implementation("org.mapstruct:mapstruct:1.5.5.Final")
  implementation("org.springframework.vault:spring-vault-core:3.1.1")
  implementation("org.springframework.plugin:spring-plugin-core:3.0.0")
  implementation("com.github.docker-java:docker-java:3.3.6") {
    exclude(group = "org.slf4j")
  }
  implementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.6")
  implementation("org.apache.httpcomponents.core5:httpcore5-h2:5.2.4") //dependency of docker-java
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.0")
  implementation("mysql:mysql-connector-java:+")
  implementation(files("inspectit-ocelot-config-2.6.4.jar"))
  implementation("io.github.oshai:kotlin-logging:6.0.9")
  implementation("io.opentelemetry:opentelemetry-api:1.37.0")

  implementation("io.github.dqualizer:dqlang:${dqlangVersion}")

  annotationProcessor("org.projectlombok:lombok")

  testImplementation("org.mockito:mockito-core:5.11.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.amqp:spring-rabbit-test")
  testImplementation("org.testcontainers:junit-jupiter:${property("testcontainersVersion")}")
  testImplementation("org.testcontainers:rabbitmq:${property("testcontainersVersion")}")
  testImplementation("org.jeasy:easy-random-core:5.0.0")
  testImplementation("com.github.fridujo:rabbitmq-mock:1.2.0")

  //testcontainers
  testImplementation("org.testcontainers:testcontainers")
  testImplementation("org.testcontainers:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("org.testcontainers:rabbitmq")
  testImplementation("org.testcontainers:mongodb")
}

dependencyManagement {
  imports {
    mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
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
