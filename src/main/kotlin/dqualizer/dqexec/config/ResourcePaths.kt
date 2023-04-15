package dqualizer.dqexec.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.nio.file.Path

/**
 * Configuration for local file paths
 */
@Configuration
class ResourcePaths {
    val scripts: Path = Path.of("poc", "scripts", "createdScript")
    val logging: Path = Path.of("poc", "logging", "logging")

    fun readResourceFile(resourcePath: String): String {
        return ClassPathResource(resourcePath).inputStream.bufferedReader().readText()
    }

    fun getScriptFilePath(counter: Int): Path {
        return scripts.resolve("$counter.js")
    }

    fun getLogFilePath(counter1: Int, counter2: Int): Path {
        return logging.resolve("$counter1-$counter2")
    }
}
