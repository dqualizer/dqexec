package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.ResourcePaths
import io.github.dqualizer.dqlang.types.adapter.k6.request.Request
import org.springframework.stereotype.Component

/** Maps the url-parameter to Javascript-Code */
@Component
class QueryParamsMapper(private val paths: ResourcePaths) : K6Mapper {
  override fun map(request: Request): String {
    val queryParams = request.queryParams!!
    val reference = queryParams.values.first()
    if (reference.isEmpty()) {
      return """
            const queryParams = {}
            const searchParams = {}

            """.trimIndent()
    }
    val queryParamsObject = paths.readResourceFile(reference)

    return """
            const queryParams = %s
            const searchParams = queryParams['params'];

            """.trimIndent().format(queryParamsObject)
  }
}
