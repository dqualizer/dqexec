package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.config.PathConfig
import dqualizer.dqexec.exception.NoReferenceFoundException
import dqualizer.dqexec.util.SafeFileReader
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component

/**
 * Maps the url-parameter to Javascript-Code
 */
@Component
@RequiredArgsConstructor
class QueryParamsMapper : k6Mapper {
    private val paths: PathConfig? = null
    private val reader: SafeFileReader? = null
    override fun map(request: Request?): String? {
        val queryParams = request!!.queryParams
        val maybeReference = queryParams.values.stream().findFirst()
        if (maybeReference.isEmpty) throw NoReferenceFoundException(queryParams)
        val referencePath = paths!!.resourcePath + maybeReference.get()
        val queryParamsObject = reader!!.readFile(referencePath)

        return """
                const queryParams = %s
                const searchParams = queryParams['params'];

                """.format(queryParamsObject);
    }
}