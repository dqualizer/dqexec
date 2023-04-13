package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.PathConfig
import dqualizer.dqexec.exception.NoReferenceFoundException
import dqualizer.dqexec.util.SafeFileReader
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component

/**
 * Maps the payload to Javascript-Code
 */
@Component
@RequiredArgsConstructor
class PayloadMapper : k6Mapper {
    private val paths: PathConfig? = null
    private val reader: SafeFileReader? = null
    override fun map(request: Request?): String? {
        val payload = request!!.payload
        val maybeReference = payload.values.stream().findFirst()
        if (maybeReference.isEmpty) throw NoReferenceFoundException(payload)
        val referencePath = paths!!.resourcePath.resolve( maybeReference.get())
        val payloadObject = reader!!.readFile(referencePath)

        return """
                const payloadData = %s
                const payloads = payloadData['payloads'];

                """.format(payloadObject);
    }
}