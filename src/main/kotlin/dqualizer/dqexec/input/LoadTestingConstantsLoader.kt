package dqualizer.dqexec.input

import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.exception.InvalidConstantsSchemaException
import io.github.dqualizer.dqlang.types.adapter.constants.loadTest.LoadTestConstants
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

/** Load a local file with load test constants */
@Configuration
class LoadTestingConstantsLoader(
  private val objectMapper: ObjectMapper,
  @Value("classpath:constant/constants.json") private val loadTestingConstants: Resource,
) {

  @Bean
  fun loadTestConstants(): LoadTestConstants {
    try {
      val resourceText = loadTestingConstants.inputStream.bufferedReader().use { it.readText() }
      return objectMapper.readValue(resourceText, LoadTestConstants::class.java)
    } catch (e: Exception) {
      throw InvalidConstantsSchemaException(e.message)
    }
  }
}
