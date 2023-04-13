package dqualizer.dqexec.config

import org.springframework.context.annotation.Configuration
import java.nio.file.Path

/**
 * Configuration for local file paths
 */
@Configuration
class PathConfig {
    val scripts: Path = Path.of("poc", "scripts", "createdScript")
    val logging: Path = Path.of("poc", "logging", "logging")

    final val resourcePath: Path = Path.of(
        this.javaClass.classLoader.getResource("")!!.toURI()
    )

    fun getScriptFilePath(counter: Int): Path {
        return resourcePath.resolve(scripts.resolve("$counter.js"))
    }

    fun getLogFilePath(counter1: Int, counter2: Int): Path {
        return resourcePath.resolve(logging.resolve("$counter1-$counter2.txt"))
    }
}
