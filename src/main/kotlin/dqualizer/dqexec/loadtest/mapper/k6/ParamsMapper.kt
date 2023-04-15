package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import org.springframework.stereotype.Component

/**
 * Maps the request-parameter to Javascript-Code
 */
@Component
class ParamsMapper(private val resourcePaths: ResourcePaths) : K6Mapper {
    override fun map(request: Request): String {
        val params = request.params
        val maybeReference = params.values.stream().findFirst()
        if (maybeReference.isEmpty) return String.format("%sconst params = {}%s", K6Mapper.newLine, K6Mapper.newLine)
        val paramsObject = resourcePaths.readResourceFile(maybeReference.get())
        return String.format(
            "%sconst params = %s%s",
            K6Mapper.newLine, paramsObject, K6Mapper.newLine
        )
    }
}