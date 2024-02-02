package dqualizer.dqexec.input

import org.springframework.context.annotation.Configuration
import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.exception.InvalidConstantsSchemaException
import io.github.dqualizer.dqlang.types.adapter.constants.LoadTestConstants
import io.github.dqualizer.dqlang.types.dam.DomainArchitectureMapping
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource

@Configuration
class DAMLoader(
  private val objectMapper: ObjectMapper,
  @Value("classpath:mapping/mapping_v3.json") private val mapping: Resource,
) {

  @Bean
  fun loadDAM(): DomainArchitectureMapping {
    try {
      val resourceText = mapping.inputStream.bufferedReader().use { it.readText() }
      return objectMapper.readValue(resourceText, DomainArchitectureMapping::class.java)
    } catch (e: Exception) {
      throw InvalidConstantsSchemaException(e.message)
    }
  }
}
