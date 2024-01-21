package dqualizer.dqexec.loadtest.mapper.k6

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqexec.exception.UnknownRequestTypeException
import io.github.dqualizer.dqlang.types.adapter.request.Request
import java.util.*
import org.springframework.stereotype.Component

/** Maps the specified request to a k6 'default function()' */
@Component
class HttpMapper(private val resourcePaths: ResourcePaths) : K6Mapper {
  override fun map(request: Request): String {
    val httpBuilder = StringBuilder()
    httpBuilder.append(exportFunctionScript())
    val path = request.path
    val type = request.type.uppercase(Locale.getDefault())
    val method =
      when (type) {
        "GET" -> "get"
        "POST" -> "post"
        "PUT" -> "put"
        "DELETE" -> "del"
        else -> throw UnknownRequestTypeException(type)
      }

    // Code for choosing one random variable for every existing path variable
    val pathVariables = request.pathVariables
    val maybeReference = pathVariables.stream().findFirst()
    if (maybeReference.isPresent) {
      val pathVariablesString =
        resourcePaths.readResourceFile(maybeReference.get().scenarios[0].path)
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

    // Code for using the request-parameter and payload inside the http method
    val payload = request.payload
    val queryParams = request.queryParams
    var extraParams = ""
    if (!payload.isEmpty() || !queryParams.isEmpty()) {
      extraParams =
        if (!payload.isEmpty() && !queryParams.isEmpty()) {
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
    val httpRequest =
      java.lang.String.format(
        "%slet response = http.%s(baseURL + `%s`%s, params);%s",
        System.lineSeparator(),
        method,
        path,
        extraParams,
        System.lineSeparator(),
      )
    httpBuilder.append(httpRequest)
    return httpBuilder.toString()
  }

  private fun exportFunctionScript(): String {
    return String.format(
      "%sexport default function() {%s",
      System.lineSeparator(),
      System.lineSeparator(),
    )
  }

  private fun randomPathVaribleScript(variable: String): String {
    return String.format(
      "%slet %s = %s_array[Math.floor(Math.random() * %s_array.length)];%s",
      System.lineSeparator(),
      variable,
      variable,
      variable,
      System.lineSeparator(),
    )
  }

  private fun randomPayloadScript(): String {
    return String.format(
      "%slet payload = payloads[Math.floor(Math.random() * payloads.length)];%s",
      System.lineSeparator(),
      System.lineSeparator(),
    )
  }

  private fun randomQueryParamScript(): String {
    return """
            let currentSearchParams = searchParams[Math.floor(Math.random() * searchParams.length)];
            let urlSearchParams = new URLSearchParams(currentSearchParams);
    """
      .trimIndent()
  }
}
