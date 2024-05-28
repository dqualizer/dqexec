package dqualizer.dqexec.resilienceTestRunner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import dqualizer.dqexec.exception.RunnerFailedException
import dqualizer.dqexec.util.EnvironmentChecker
import dqualizer.dqexec.config.StartupConfig
import io.github.dqualizer.dqlang.types.adapter.ctk.CtkConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.util.logging.Logger

/**
 *
 */
@Service
class CtkRunner(private val startupConfig: StartupConfig, @Value("\${dqualizer.dqexec.resilienceExecutionApi.host}") private val resilienceExecutionApiHost: String,
        @Value("\${dqualizer.dqexec.resilienceExecutionApi.port}") private val resilienceExecutionApiPort: String)
{
    private val logger = Logger.getLogger(this.javaClass.name)


    fun start(config: CtkConfiguration) {
        logger.info("### CTK CONFIGURATION RECEIVED ###")
        try {
            this.run(config)
        } catch (e: Exception) {
            logger.severe("### Resilience TESTING FAILED ###")
            e.printStackTrace()
            throw RunnerFailedException(e.message!!)
        }
    }

    /**
     * Read the configuration and start with the text execution
     *
     * @param config Received inofficial k6-configuration
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun run(config: CtkConfiguration) {
        logger.info("Trying to run configuration: " + ObjectMapper().writeValueAsString(config))

        var testCounter = 1
        val objectMapper = ObjectMapper()
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT)

        for (chaosExperiment in config.ctkChaosExperiments) {
            val experimentJsonPayload = objectMapper.writeValueAsString(chaosExperiment)

            logger.info("### CHAOS EXPERIMENT $testCounter WAS CREATED ###")

            val exitValue = requestExperimentExecutionOnHost(experimentJsonPayload)
            logger.info(" CHAOS EXPERIMENT $testCounter FINISHED WITH VALUE $exitValue ###")
        }
        logger.info("### RESILIENCE TESTING COMPLETE ###")
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun requestExperimentExecutionOnHost(experimentJson: String): Int {

        val restTemplate = RestTemplate()
        var url = ""
        if (EnvironmentChecker.isRunningInDocker){
            url = "http://host.docker.internal:$resilienceExecutionApiPort/execute_experiment"
        }
        else{
            url = "http://$resilienceExecutionApiHost:$resilienceExecutionApiPort/execute_experiment"
        }

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("dbUsername", startupConfig.getDbUsername())
        headers.set("dbPassword", startupConfig.getDbPassword())
        headers.set("username", startupConfig.getUsername())
        headers.set("password", startupConfig.getPassword())

        val httpEntity = HttpEntity(experimentJson, headers)

        data class CtkExperimentExecutorAPIResponse(val status_code: Int, val status: String, val info:String)

        val response: CtkExperimentExecutorAPIResponse? = restTemplate.postForObject(url, httpEntity, CtkExperimentExecutorAPIResponse::class.java)

        val objectMapper = ObjectMapper()
        val responseMap = objectMapper.convertValue(response, Map::class.java)
        val statusCode = responseMap["status_code"]
        val status = responseMap["status"]
        val info = responseMap["info"]
        if (statusCode !is Int || statusCode != 200) {
            throw Exception("Following non 200 or non integer exit code returned from host experimentExecutor API: $statusCode || status: $status || info: $info")
        }

        else{
            logger.info(
            """
            ### Chaos Experiment finished successfully and returned status code: $statusCode ###
            ### and returned following status message: $status ###
            ### error trace: $info ###
            """.trimIndent()
            )
        }
        return statusCode
    }
}
