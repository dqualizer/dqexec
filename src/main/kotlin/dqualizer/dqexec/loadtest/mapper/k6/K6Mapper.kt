package dqualizer.dqexec.loadtest.mapper.k6

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.dqualizer.dqlang.types.adapter.request.Request

/** An interface for all mappers necessary for creating a k6-script */
interface K6Mapper {
  /**
   * Map one part of request object to a String, which can be written inside a Javascript file
   *
   * @param request
   * @return String that can be written inside a Javascript file
   */
  fun map(request: Request): String

  companion object {
    @JvmField val newLine = System.lineSeparator()

    @JvmField val objectMapper = ObjectMapper()
  }
}
