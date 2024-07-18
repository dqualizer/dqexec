package dqualizer.dqexec.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.nio.file.Path

/**
 * Configuration for local file paths
 */
@Configuration
class ResourcePaths(
  @Value("\${dqualizer.dqexec.output.logs:logs}")
  loggingPathStr: String,
  @Value("\${dqualizer.dqexec.output.loadtest_configurations:loadtest_configurations}")
  loadtestConfigurationsPathStr: String,
) {
  val scripts: Path = Path.of(loadtestConfigurationsPathStr)
  val logging: Path = Path.of(loggingPathStr)

  fun readResourceFile(resourcePath: String): String {
    return ClassPathResource(resourcePath).inputStream.bufferedReader().readText()
  }

  fun getScriptFilePath(scriptID: Int): Path {
    return scripts.resolve("createdScript$scriptID.js")
  }

  fun getLogFilePath(testID: Int, runID: Int): Path {
    return logging.resolve("test$testID-run$runID")
  }
}
