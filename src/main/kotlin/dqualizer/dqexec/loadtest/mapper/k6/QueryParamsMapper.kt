package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqexec.exception.NoReferenceFoundException
import io.github.dqualizer.dqlang.types.adapter.request.Request
import org.springframework.stereotype.Component

/**
 * Maps the url-parameter to Javascript-Code
 */
@Component
class QueryParamsMapper(private val paths: ResourcePaths) : RuntimeQualityAnalysisConfigurationTranslator {

    override fun map(request: Request): String {
        val queryParams = request.queryParams
        // val maybeReference = queryParams.stream().findFirst()
        return String.format("%sconst queryParams = {}%s", K6Mapper.newLine, K6Mapper.newLine)

/*        val queryParamsObject = paths.readResourceFile(queryParams.get(0).toString())

        return """
                const queryParams = %s
                const searchParams = queryParams['params'];

                """.trimIndent().format(queryParamsObject)*/
    }
}
