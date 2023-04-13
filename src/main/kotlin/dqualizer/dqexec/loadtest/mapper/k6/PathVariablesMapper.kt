package dqualizer.dqexec.loadtest.mapper.k6

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqexec.exception.NoReferenceFoundException
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import org.springframework.stereotype.Component

/**
 * Maps the path variables to Javascript-Code
 */
@Component
class PathVariablesMapper(private val resourcePaths: ResourcePaths) : K6Mapper {

    override fun map(request: Request): String {
        val pathVariablesBuilder = StringBuilder()
        val pathVariables = request.pathVariables
        val maybeReference = pathVariables.values.stream().findFirst()
        if (maybeReference.isEmpty) throw NoReferenceFoundException(pathVariables)
        val pathVariablesString = resourcePaths.readResourceFile(maybeReference.get())
        val pathVariablesScript = String.format(
            "%sconst path_variables = %s",
            K6Mapper.newLine, pathVariablesString
        )
        pathVariablesBuilder.append(pathVariablesScript)
        try {
            val node = ObjectMapper().readTree(pathVariablesString)
            val variables = node.fieldNames()
            while (variables.hasNext()) {
                val variable = variables.next()
                val particularPathVariablesScript = String.format(
                    "%sconst %s_array = path_variables['%s'];",
                    K6Mapper.newLine, variable, variable
                )
                pathVariablesBuilder.append(particularPathVariablesScript)
            }
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
        return pathVariablesBuilder.toString()
    }
}
