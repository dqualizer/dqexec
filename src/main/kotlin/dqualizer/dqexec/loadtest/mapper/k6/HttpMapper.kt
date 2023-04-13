package dqualizer.dqexec.loadtest.mapper.k6

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.config.PathConfig
import dqualizer.dqexec.exception.UnknownRequestTypeException
import dqualizer.dqexec.util.SafeFileReader
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component
import java.util.*

/**
 * Maps the specified request to a k6 'default function()'
 */
@Component
@RequiredArgsConstructor
class HttpMapper : k6Mapper {
    private val paths: PathConfig? = null
    private val reader: SafeFileReader? = null
    override fun map(request: Request?): String? {
        val httpBuilder = StringBuilder()
        httpBuilder.append(exportFunctionScript())
        val path = request!!.path
        val type = request.type.uppercase(Locale.getDefault())
        val method = when
            (type) {
            "GET" -> "get"
            "POST" -> "post"
            "PUT" -> "put"
            "DELETE" -> "del"
            else -> throw UnknownRequestTypeException(type)
        }

        //Code for choosing one random variable for every existing path variable
        val pathVariables = request.pathVariables
        val maybeReference = pathVariables.values.stream().findFirst()
        if (maybeReference.isPresent) {
            val referencePath = paths!!.resourcePath.resolve(maybeReference.get())

            val pathVariablesString = reader!!.readFile(referencePath)
            try {
                val node = ObjectMapper().readTree(pathVariablesString)
                val variables = node.fieldNames()
                while (variables.hasNext()) {
                    val variable = variables.next()
                    val randomPathVariable = randomPathVaribleScript(variable)
                    httpBuilder.append(randomPathVariable)
                }
            } catch (e: JsonProcessingException) {
                e.printStackTrace()
            }
        }

        //Code for using the request-parameter and payload inside the http method
        val payload = request.payload
        val queryParams = request.queryParams
        var extraParams = ""
        if (!payload.isEmpty() || !queryParams.isEmpty()) {
            extraParams = if (!payload.isEmpty() && !queryParams.isEmpty()) {
                httpBuilder.append(randomQueryParamScript())
                httpBuilder.append(randomPayloadScript())
                " + `?\${urlSearchParams.toString()}`, JSON.stringify(payload)"
            } else if (!payload.isEmpty()) {
                httpBuilder.append(randomPayloadScript())
                ", JSON.stringify(payload)"
            } else {
                httpBuilder.append(randomQueryParamScript())
                " + `?\${urlSearchParams.toString()}`"
            }
        }
        val httpRequest = java.lang.String.format(
            "%slet response = http.%s(baseURL + `%s`%s, params);%s",
            k6Mapper.newLine, method, path, extraParams, k6Mapper.newLine
        )
        httpBuilder.append(httpRequest)
        return httpBuilder.toString()
    }

    private fun exportFunctionScript(): String {
        return String.format(
            "%sexport default function() {%s",
            k6Mapper.newLine, k6Mapper.newLine
        )
    }

    private fun randomPathVaribleScript(variable: String): String {
        return String.format(
            "%slet %s = %s_array[Math.floor(Math.random() * %s_array.length)];%s",
            k6Mapper.newLine, variable, variable, variable, k6Mapper.newLine
        )
    }

    private fun randomPayloadScript(): String {
        return String.format(
            "%slet payload = payloads[Math.floor(Math.random() * payloads.length)];%s",
            k6Mapper.newLine, k6Mapper.newLine
        )
    }

    private fun randomQueryParamScript(): String {
        return """
                let currentSearchParams = searchParams[Math.floor(Math.random() * searchParams.length)];
                let urlSearchParams = new URLSearchParams(currentSearchParams);
                """;
    }
}