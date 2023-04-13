package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqexec.exception.NoReferenceFoundException
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import org.springframework.stereotype.Component

/**
 * Maps the payload to Javascript-Code
 */
@Component
class PayloadMapper(private val resourcePaths: ResourcePaths) : K6Mapper {

    override fun map(request: Request): String {
        val payload = request.payload
        val maybeReference = payload.values.stream().findFirst()
        if (maybeReference.isEmpty) throw NoReferenceFoundException(payload)
        val payloadObject = resourcePaths.readResourceFile(maybeReference.get())

        return """
                const payloadData = %s
                const payloads = payloadData['payloads'];

                """.format(payloadObject);
    }
}
