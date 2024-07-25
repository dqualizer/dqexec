package dqualizer.dqexec.input

import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.exception.InvalidConstantsSchemaException
import dqualizer.dqexec.util.EnvironmentChecker
import io.github.dqualizer.dqlang.types.adapter.constants.resilienceTest.ResilienceTestConstants
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.nio.file.Files
import kotlin.io.path.Path

/**
 * Load a local file with resilience test constants
 */
@Configuration
class ResilienceTestingConstantsLoader(
    private val objectMapper: ObjectMapper,
    @Value("classpath:constant/resilience_testing_constants.json")
    private val resilienceTestingConstants: Resource
) {
    @Bean
    fun createResilienceTestConstants(): ResilienceTestConstants {
        try {
            if (EnvironmentChecker.isRunningInDocker){
                val constantsPath = Path("/app/input_ressources/resilience_testing_constants.json")
                return objectMapper.readValue(Files.readString(constantsPath), ResilienceTestConstants::class.java)
            }
            val resourceText = resilienceTestingConstants.inputStream.bufferedReader().use { it.readText() }
            return objectMapper.readValue(resourceText, ResilienceTestConstants::class.java)
        } catch (e: Exception) {
            throw InvalidConstantsSchemaException(e.message)
        }
    }
}
