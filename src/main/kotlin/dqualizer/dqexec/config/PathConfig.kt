package dqualizer.dqexec.config

import org.springframework.context.annotation.Configuration
import java.nio.file.Path

/**
 * Configuration for local file paths
 */
@Configuration
class PathConfig {
    val scripts = Path.of("poc", "scripts", "createdScript")
    val logging = Path.of("poc", "logging", "logging")

    final val resourcePath = Path.of(
        this.javaClass.classLoader.getResource("")!!.toURI()
    )

    val constants: Path = resourcePath.resolve(Path.of("constants", "constants.json"))

    fun getScript(counter: Int): Path {
        return resourcePath.resolve(scripts.resolve("$counter.js"))
    }

    fun getLogging(counter1: Int, counter2: Int): Path {
        return resourcePath.resolve(logging.resolve("$counter1-$counter2.txt"))
    }
}
