package dqualizer.dqexec.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.nio.file.Path

/**
 * Configuration for local file paths
 */
@Configuration
class PathConfig(
    @Value("classpath:") val classPath: Resource
) {
    val scripts: Path = Path.of("poc", "scripts", "createdScript")
    val logging: Path = Path.of("poc", "logging", "logging")

    fun getResourcesPath(): Path {
        return Path.of(classPath.file.path)
    }

    fun getScriptFilePath(counter: Int): Path {
        return getResourcesPath().resolve(scripts.resolve("$counter.js"))
    }

    fun getLogFilePath(counter1: Int, counter2: Int): Path {
        return getResourcesPath().resolve(logging.resolve("$counter1-$counter2.txt"))
    }
}
