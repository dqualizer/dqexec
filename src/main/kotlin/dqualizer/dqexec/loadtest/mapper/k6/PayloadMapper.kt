package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqexec.exception.NoReferenceFoundException
import io.github.dqualizer.dqlang.types.adapter.request.Request
import org.springframework.stereotype.Component

/**
 * Maps the payload to Javascript-Code
 */
@Component
class PayloadMapper(private val resourcePaths: ResourcePaths) : RuntimeQualityAnalysisConfigurationTranslator {

    override fun map(request: Request): String {
        val payload = request.payload
        val maybeReference = payload[0].scenarios[0].path
        // if (maybeReference.isEmpty) throw NoReferenceFoundException(payload)
        val payloadObject = resourcePaths.readResourceFile(maybeReference.toString())

        return """
                const payloadData = %s
                const payloads = payloadData['payloads'];

                """.trimIndent().format(payloadObject)
    }
}
