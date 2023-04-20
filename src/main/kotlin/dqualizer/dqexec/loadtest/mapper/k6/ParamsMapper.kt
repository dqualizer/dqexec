package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import org.springframework.stereotype.Component

/**
 * Maps the request-parameter to Javascript-Code
 */
@Component
class ParamsMapper(private val resourcePaths: ResourcePaths) : RuntimeQualityAnalysisConfigurationTranslator {
    override fun map(request: Request): String {
        val params = request.params
        val maybeReference = params.values.stream().findFirst()
        if (maybeReference.isEmpty) return String.format("%nconst params = {}%n")
        val paramsObject = resourcePaths.readResourceFile(maybeReference.get())
        return String.format("%nconst params = %s%n", paramsObject)
    }
}
