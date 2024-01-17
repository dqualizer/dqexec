package dqualizer.dqexec.loadtest.mapper.k6

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.config.ResourcePaths
import io.github.dqualizer.dqlang.types.adapter.request.Request
import org.springframework.stereotype.Component

/**
 * Maps the path variables to Javascript-Code
 */
@Component
class PathVariablesMapper(private val resourcePaths: ResourcePaths) : RuntimeQualityAnalysisConfigurationTranslator {

    override fun map(request: Request): String {
        val pathVariablesBuilder = StringBuilder()
        val pathVariables = request.pathVariables
        val maybeReference = pathVariables.stream().findFirst()
        val pathVariablesString = resourcePaths.readResourceFile("auftrag/auftragsnummern/angelegt.json")
        val pathVariablesScript = String.format(
            "%sconst path_variables = %s",
           System.lineSeparator(), pathVariablesString
        )
        pathVariablesBuilder.append(pathVariablesScript)
        try {
            val node = ObjectMapper().readTree(pathVariablesString)
            val variables = node.fieldNames()
            while (variables.hasNext()) {
                val variable = variables.next()
                val particularPathVariablesScript = String.format(
                    "%sconst %s_array = path_variables['%s'];",
                   System.lineSeparator(), variable, variable
                )
                pathVariablesBuilder.append(particularPathVariablesScript)
            }
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
        return pathVariablesBuilder.toString()
    }
}
