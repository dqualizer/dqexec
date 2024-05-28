package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.ResourcePaths
import io.github.dqualizer.dqlang.types.adapter.k6.request.Request
import org.springframework.stereotype.Component

/** Maps the payload to Javascript-Code */
@Component
class PayloadMapper(private val resourcePaths: ResourcePaths) : K6Mapper {
  override fun map(request: Request): String {
    val payload = request.payload!!
    val reference = payload.values.firstOrNull()

    if (reference.isNullOrEmpty()) {
      return String.format("%sconst payloadData = {}%s", K6Mapper.newLine, K6Mapper.newLine)
    }

    val payloadObject = resourcePaths.readResourceFile(reference)

    return """
            const payloadData = %s
            const payloads = payloadData['payloads'];

    """
      .trimIndent()
      .format(payloadObject)
  }
}
