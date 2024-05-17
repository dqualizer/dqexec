package dqualizer.dqexec.config

import dqualizer.dqexec.util.EnvironmentChecker
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
class ResourcePaths () {
    val scripts: Path = Path.of("scripts")
    val logging: Path = Path.of("logging")
    val experiments: Path = Path.of("generated_experiments")


    fun readResourceFile(resourcePath: String): String {

        if (EnvironmentChecker.isRunningInDocker){
            val ressourcePath = Path("/app/input_ressources/$resourcePath")
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

    fun getExperimentFilePath(title:String): Path {
        return experiments.resolve(title + "_experiment.json")
    }

}
