package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.ResourcePaths
import io.github.dqualizer.dqlang.types.adapter.k6.request.Request
import org.springframework.stereotype.Component

/** Maps the request-parameter to Javascript-Code */
@Component
class ParamsMapper(private val resourcePaths: ResourcePaths) : K6Mapper {
  override fun map(request: Request): String {
    val params: Map<String, String> = request.params!!
    val path = params.values.first()

    if (path.isEmpty()) {
      return String.format("%sconst params = {}%s", K6Mapper.newLine, K6Mapper.newLine)
    }

    val paramsObject = resourcePaths.readResourceFile(path)
    return String.format(
      "%sconst params = %s%s",
      K6Mapper.newLine,
      paramsObject,
      K6Mapper.newLine,
    )
  }
}
