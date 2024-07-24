package dqualizer.dqexec.resilience

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import dqualizer.dqexec.exception.RunnerFailedException
import dqualizer.dqexec.util.EnvironmentChecker
import io.github.dqualizer.dqlang.types.adapter.ctk.CtkConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.util.logging.Logger

@Service
class CtkRunner(
  @Value("\${dqualizer.dqexec.resilienceExecutionApi.host}")
  private val resilienceExecutionApiHost: String,
  @Value("\${dqualizer.dqexec.resilienceExecutionApi.port}")
  private val resilienceExecutionApiPort: String,
  private val objectMapper: ObjectMapper)
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
     * @param config Received inofficial ctk-configuration
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun run(config: CtkConfiguration) {
      logger.info("Trying to run configuration: " + ObjectMapper().writeValueAsString(config))

      var testCounter = 1
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT)

      for (chaosExperiment in config.ctkChaosExperiments) {
        val experimentJsonPayload = objectMapper.writeValueAsString(chaosExperiment)

        logger.info("### CHAOS EXPERIMENT $testCounter WAS CREATED ###")

        val exitValue = requestExperimentExecutionOnHost(experimentJsonPayload)
        logger.info(" CHAOS EXPERIMENT $testCounter FINISHED WITH VALUE $exitValue ###")
        testCounter++
      }
      logger.info("### RESILIENCE TESTING COMPLETE ###")
    }

  /**
   * Executes the experiment by sending an HTTP request to a REST API
   * The REST API is provided by this project: https://github.com/LeHenningo/Dqualizer-CTK
   * and executes the experiment on the SUT
   */
    @Throws(IOException::class, InterruptedException::class)
    private fun requestExperimentExecutionOnHost(experimentJson: String): Int {
      val restTemplate = RestTemplate()
      val url = if (EnvironmentChecker.isRunningInDocker) {
        "http://host.docker.internal:$resilienceExecutionApiPort/execute_experiment"
      } else {
        "http://$resilienceExecutionApiHost:$resilienceExecutionApiPort/execute_experiment"
      }

      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      // hardcoded secrets to enable authentication in python scripts against per default configured credentials in mySql
      headers.set("dbUsername", "aDBUser")
      headers.set("dbPassword", "aDBPw")
      headers.set("username", "demoUser")
      headers.set("password", "demo")

      val httpEntity = HttpEntity(experimentJson, headers)

      val response: CtkExperimentExecutorAPIResponse? = restTemplate.postForObject(url, httpEntity, CtkExperimentExecutorAPIResponse::class.java)

      val responseMap = objectMapper.convertValue(response, Map::class.java)
      val statusCode = responseMap["statusCode"]
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
