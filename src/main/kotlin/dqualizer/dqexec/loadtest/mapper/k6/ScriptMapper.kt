package dqualizer.dqexec.loadtest.mapper.k6

import com.fasterxml.jackson.core.JsonProcessingException
import io.github.dqualizer.dqlang.types.adapter.k6.K6LoadTest
import io.github.dqualizer.dqlang.types.adapter.options.Options
import io.github.dqualizer.dqlang.types.adapter.request.Request
import io.github.dqualizer.dqlang.types.dam.Payload
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.context.IContext
import org.thymeleaf.spring6.SpringTemplateEngine
import java.util.*

/**
 * Maps the one loadtest from the inofficial k6-configuration to k6-script
 */
@Component
class ScriptMapper(
    private val paramsMapper: ParamsMapper,
    private val payloadMapper: PayloadMapper,
    private val queryParamsMapper: QueryParamsMapper,
    private val httpMapper: HttpMapper,
    private val checksMapper: ChecksMapper,
    private val pathVariablesMapper: PathVariablesMapper
) : K6Mapper {

    /**
     * Map one loadtest to a k6-script
     *
     * @param baseURL  The baseURL for all loadtests inside the inofficial k6-configuration
     * @param loadTest One specified loadtest
     * @return A list of strings, which can be written to a file
     * @throws JsonProcessingException
     */
    @Throws(JsonProcessingException::class)
    fun getScript(baseURL: String, loadTest: K6LoadTest): List<String> {
        val script: MutableList<String> = LinkedList()
        val options = loadTest.options
        script.add(startScript(baseURL, options))
        var request = loadTest.request
        val requestScript = this.map(request)
        script.add(requestScript)
        script.add("}")
        return script
    }

    override fun map(request: Request): String {

        val requestBuilder = StringBuilder()
        val paramsScript = paramsMapper.map(request)
        requestBuilder.append(paramsScript)
        if (request.payload.isNotEmpty()) {
            val payloadScript = payloadMapper.map(request)
            requestBuilder.append(payloadScript)
        }
        if (request.queryParams.isNotEmpty()) {
            val queryParamsScript = queryParamsMapper.map(request)
            requestBuilder.append(queryParamsScript)
        }
        if (request.pathVariables.isNotEmpty()) {
            val pathVariablesScript = pathVariablesMapper.map(request)
            requestBuilder.append(pathVariablesScript)
        }
        val httpScript = httpMapper.map(request)
        requestBuilder.append(httpScript)
        requestBuilder.append(trackDataPerURLScript())
        if (request.checks != null) {
            val checksScript = checksMapper.map(request)
            requestBuilder.append(checksScript)
        }
        requestBuilder.append(sleepScript())
        return requestBuilder.toString()
    }

    /**
     * Write a the beginning of Javascript-file
     *
     * @param baseURL The baseURL for all loadtests inside the inofficial k6-configuration
     * @param options The k6 'options' object
     * @return String that can be written at the beginning of a Javascript file
     * @throws JsonProcessingException
     */
    @Throws(JsonProcessingException::class)
    private fun startScript(baseURL: String, options: Options): String {
        val optionsString = K6Mapper.objectMapper.writeValueAsString(options)
        val trackDataPerURL = trackDataPerURLInitScript()
        return """
                import http from 'k6/http';
                import {URLSearchParams} from 'https://jslib.k6.io/url/1.0.0/index.js';
                import {check, sleep} from 'k6';
                %s

                let baseURL = '%s';

                export let options = %s;

                """.trimIndent().format(trackDataPerURL, baseURL, optionsString)
    }

    /**
     * Write functions to track data for every url used during the loadtest
     *
     * @return String that can be written inside a Javascript file
     */
    private fun trackDataPerURLInitScript(): String {
        return """
                import {Counter} from 'k6/metrics';

                export const epDataSent = new Counter('data_sent_endpoint');
                export const epDataRecv = new Counter('data_received_endpoint');

                function sizeOfHeaders(headers) {
                    return Object.keys(headers).reduce((sum, key) => sum + key.length + headers[key].length, 0);
                }

                function trackDataMetricsPerURL(res) {
                    epDataSent.add(sizeOfHeaders(res.request.headers) + res.request.body.length, { url: res.url });
                    epDataRecv.add(sizeOfHeaders(res.headers) + res.body.length, { url: res.url });
                }
                """.trimIndent()
    }

    private fun trackDataPerURLScript(): String {
        return String.format("trackDataMetricsPerURL(response);%s",System.lineSeparator())
    }

    private fun sleepScript(): String {
        val random = Random()
        val duration = random.nextInt(5) + 1
        return String.format(
            "sleep(%d);%s",
            duration,System.lineSeparator()
        )
    }
}
