package dqualizer.dqexec.input

import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.exception.InvalidConstantsSchemaException
import io.github.dqualizer.dqlang.types.adapter.constants.LoadTestConstants
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * Load a local file with load test constants
 */
@Configuration
class LoadTestingConstantsLoader(
    private val objectMapper: ObjectMapper,
    @Value("classpath:constant/constants.json") private val loadTestingConstants: Resource
) {
    @Bean
    fun loadTestConstants(): LoadTestConstants {
        try {
            val isRunningInDocker = Path("/proc/1/cgroup").exists()
            if (isRunningInDocker){
                val constantsPath = Path("/app/input_ressources/constants.json")
                return objectMapper.readValue(Files.readString(constantsPath), LoadTestConstants::class.java)
            }
            val resourceText = loadTestingConstants.inputStream.bufferedReader().use { it.readText() }
            return objectMapper.readValue(resourceText, LoadTestConstants::class.java)
        } catch (e: Exception) {
            throw InvalidConstantsSchemaException(e.message)
        }
    }
}
