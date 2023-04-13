package dqualizer.dqexec.adapter

import dqualizer.dqexec.exception.UnknownTermException
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.LoadTestConstants
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Checks
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.Endpoint
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.ResponseMeasure
import org.springframework.stereotype.Component
import java.util.*
import java.util.regex.Pattern

/**
 * Adapts one endpoint to a Request object
 */
@Component
class EndpointAdapter(private val loadtestConstants: LoadTestConstants) {

    /**
     * @param endpoint        Endpoint for one loadtest
     * @param responseMeasure Information for response measures
     * @return An inoffical k6 Request object
     */
    fun adaptEndpoint(endpoint: Endpoint, responseMeasure: ResponseMeasure): Request {
        val field = endpoint.field
        val path = markPathVariables(field)
        val type = endpoint.operation
        val pathVariables = endpoint.pathVariables
        val queryParams = endpoint.urlParameter
        val params = endpoint.requestParameter
        val payload = endpoint.payload
        val duration: Int = this.getDuration(responseMeasure)
        val statusCodes = getStatusCodes(endpoint)
        val checks = Checks(statusCodes, duration)
        return Request(type, path, pathVariables, queryParams, params, payload, checks)
    }

    /**
     * At first the method finds all path variables inside the field with the help of a regex pattern.
     * The pattern is looking for text that is enclosed by curly brackets {}.
     * Those variables are saved inside a list, including the brackets. Duplicates will be combined to one variable.
     * After that a "$"-symbol will be added to all found variables inside the field
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
        return variables.stream()
            .distinct()
            .reduce(field) { path: String, variable: String -> path.replace(variable, "$$variable") }
    }

    /**
     * Get the expected answer duration for this request specified in the modeling
     *
     * @param responseMeasure Information for response measures
     * @return The expected answer duration for this request
     */
    private fun getDuration(responseMeasure: ResponseMeasure): Int {
        val responseTime = loadtestConstants.responseTime
        val responseTimeValue = responseMeasure.responseTime
        return when (responseTimeValue) {
            "SATISFIED" -> responseTime.satisfied
            "TOLERATED" -> responseTime.tolerated
            "FRUSTRATED" -> responseTime.frustrated
            else -> throw UnknownTermException(responseTimeValue)
        }
    }

    /**
     * Get a set of expected status codes for this request
     *
     * @param endpoint Endpoint for this request
     * @return A set of expected status codes
     */
    private fun getStatusCodes(endpoint: Endpoint): LinkedHashSet<Int> {
        val responses = endpoint.responses
        val statusCodes = LinkedHashSet<Int>()
        for (response in responses) {
            val statusCode = response.expectedCode
            statusCodes.add(statusCode)
        }
        return statusCodes
    }
}