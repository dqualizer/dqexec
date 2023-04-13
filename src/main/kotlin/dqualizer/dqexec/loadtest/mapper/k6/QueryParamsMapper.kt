package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqexec.exception.NoReferenceFoundException
import dqualizer.dqexec.util.SafeFileReader
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import org.springframework.stereotype.Component

/**
 * Maps the url-parameter to Javascript-Code
 */
@Component
class QueryParamsMapper(private val reader: SafeFileReader, private val paths: ResourcePaths) : K6Mapper {

    override fun map(request: Request): String {
        val queryParams = request.queryParams
        val maybeReference = queryParams.values.stream().findFirst()
        if (maybeReference.isEmpty) throw NoReferenceFoundException(queryParams)
        val referencePath = paths.getResourcesPath().resolve(maybeReference.get())
        val queryParamsObject = reader.readFile(referencePath)

        return """
                const queryParams = %s
                const searchParams = queryParams['params'];

                """.format(queryParamsObject);
    }
}
