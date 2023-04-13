package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.PathConfig
import dqualizer.dqexec.util.SafeFileReader
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import org.springframework.stereotype.Component

/**
 * Maps the request-parameter to Javascript-Code
 */
@Component
class ParamsMapper(private val reader: SafeFileReader, private val paths: PathConfig) : K6Mapper {
    override fun map(request: Request): String {
        val params = request.params
        val maybeReference = params.values.stream().findFirst()
        if (maybeReference.isEmpty) return String.format("%sconst params = {}%s", K6Mapper.newLine, K6Mapper.newLine)
        val referencePath = paths.getResourcesPath().resolve(maybeReference.get())
        val paramsObject = reader.readFile(referencePath)
        return String.format(
            "%sconst params = %s%s",
            K6Mapper.newLine, paramsObject, K6Mapper.newLine
        )
    }
}
