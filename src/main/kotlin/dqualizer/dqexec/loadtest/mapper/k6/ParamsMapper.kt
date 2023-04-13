package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.PathConfig
import dqualizer.dqexec.util.SafeFileReader
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component

/**
 * Maps the request-parameter to Javascript-Code
 */
@Component
@RequiredArgsConstructor
class ParamsMapper : k6Mapper {
    private val paths: PathConfig? = null
    private val reader: SafeFileReader? = null
    override fun map(request: Request?): String? {
        val params = request!!.params
        val maybeReference = params.values.stream().findFirst()
        if (maybeReference.isEmpty) return String.format("%sconst params = {}%s", k6Mapper.newLine, k6Mapper.newLine)
        val referencePath = paths!!.resourcePath + maybeReference.get()
        val paramsObject = reader!!.readFile(referencePath)
        return String.format(
            "%sconst params = %s%s",
            k6Mapper.newLine, paramsObject, k6Mapper.newLine
        )
    }
}