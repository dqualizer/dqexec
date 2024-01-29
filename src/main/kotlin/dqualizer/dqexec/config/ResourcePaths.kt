package dqualizer.dqexec.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * Configuration for local file paths
 */
@Configuration
class ResourcePaths {
    val scripts: Path = Path.of("scripts")
    val logging: Path = Path.of("logging")

    fun readResourceFile(resourcePath: String): String {
        val isRunningInDocker = Path("/proc/1/cgroup").exists()
        if (isRunningInDocker){
            val ressourcePath = Path("/app/request_params/$resourcePath")
            return Files.readString(ressourcePath)
        }
    return ClassPathResource(resourcePath).inputStream.bufferedReader().readText()
    }

    fun getScriptFilePath(scriptID: Int): Path {
        return scripts.resolve("createdScript$scriptID.js")
    }

    fun getLogFilePath(testID: Int, runID: Int): Path {
        return logging.resolve("test$testID-run$runID")
    }
}
