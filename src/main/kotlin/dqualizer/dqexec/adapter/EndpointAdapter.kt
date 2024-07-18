package dqualizer.dqexec.adapter

import dqualizer.dqexec.exception.UnknownTermException
import io.github.dqualizer.dqlang.types.adapter.constants.LoadTestConstants
import io.github.dqualizer.dqlang.types.adapter.k6.request.Checks
import io.github.dqualizer.dqlang.types.adapter.k6.request.Request
import io.github.dqualizer.dqlang.types.dam.architecture.RESTEndpoint
import io.github.dqualizer.dqlang.types.dam.architecture.RESTEndpoint.EndpointParameter
import io.github.dqualizer.dqlang.types.dam.architecture.RESTEndpoint.EndpointParameterType
import io.github.dqualizer.dqlang.types.rqa.definition.enums.ResponseTime
import io.github.dqualizer.dqlang.types.rqa.definition.loadtest.ResponseMeasures
import org.springframework.stereotype.Component
import java.util.*
import java.util.regex.Pattern


/** Adapts one endpoint to a Request object */
@Component
class EndpointAdapter(private val loadtestConstants: LoadTestConstants) {
  /**
   * @param endpoint Endpoint for one loadtest
   * @param responseMeasure Information for response measures
   * @return An inoffical k6 Request object
   */
  fun adaptEndpoint(endpoint: RESTEndpoint, responseMeasure: ResponseMeasures): Request {
    val field = endpoint.route
    val path = markPathVariables(field)
    val type = endpoint.methods.first().name

    val parameter = endpoint.parameter
    val pathVariables = parameter.firstOrNull { it.type!! == EndpointParameterType.PathVariable }
    val queryParams = parameter.firstOrNull { it.type!! == EndpointParameterType.QueryParameter }
    val params = parameter.firstOrNull { it.type!! == EndpointParameterType.Header }
    val payload = parameter.firstOrNull { it.type!! == EndpointParameterType.RequestBody }

    val duration: Int = this.getDuration(responseMeasure)
    val statusCodes = getStatusCodes(endpoint)
    val checks = Checks(statusCodes, duration)

    return Request(
      type,
      path,
      convertToMap(pathVariables),
      convertToMap(queryParams),
      convertToMap(params),
      convertToMap(payload),
      checks
    )
  }

  private fun convertToMap(parameter: EndpointParameter?): Map<String, String> {
    if (parameter == null) return emptyMap()
    return mapOf(parameter.type.toString() to parameter.data!!)
  }

  /**
   * At first the method finds all path variables inside the field with the help of a regex pattern.
   * The pattern is looking for text that is enclosed by curly brackets {}. Those variables are
   * saved inside a list, including the brackets. Duplicates will be combined to one variable. After
   * that a "$"-symbol will be added to all found variables inside the field
   *
   * @param field Path with unmarked variables
   * @return Path with marked variables, for example '{id}' turns into '${id}'
   */
  private fun markPathVariables(field: String): String {
    val pattern = Pattern.compile("\\{.*?}")
    val matcher = pattern.matcher(field)
    val variables: MutableList<String> = LinkedList()

    while (matcher.find()) {
      val foundVariable = matcher.group()
      variables.add(foundVariable)
    }
    return variables.stream().distinct().reduce(field) { path: String, variable: String ->
      path.replace(variable, "$$variable")
    }
  }

  /**
   * Get the expected answer duration for this request specified in the modeling
   *
   * @param responseMeasure Information for response measures
   * @return The expected answer duration for this request
   */
  private fun getDuration(responseMeasure: ResponseMeasures): Int {
    val responseTime = loadtestConstants.responseTime

    return when (val responseTimeValue = responseMeasure.responseTime) {
      ResponseTime.SATISFIED -> responseTime.satisfied
      ResponseTime.TOLERATED -> responseTime.tolerated
      ResponseTime.FRUSTRATED -> responseTime.frustrated
      else -> throw UnknownTermException(responseTimeValue.toString())
    }
  }

  /**
   * Get a set of expected status codes for this request
   *
   * @param endpoint Endpoint for this request
   * @return A set of expected status codes
   */
  private fun getStatusCodes(endpoint: RESTEndpoint): LinkedHashSet<Int> {
    val responses = endpoint.responseDescription!!
    val statusCodes = LinkedHashSet<Int>()
    statusCodes.addAll(responses.expectedStatusCodes)

    return statusCodes
  }
}
