package dqualizer.dqexec.resilienceTestRunner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqexec.config.StartupConfig
import dqualizer.dqexec.exception.RunnerFailedException
import dqualizer.dqexec.util.ProcessLogger
import io.github.dqualizer.dqlang.types.adapter.ctk.CtkConfiguration
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 *
 */
@Service
class CtkRunner(
        private val processLogger: ProcessLogger,
        private val paths: ResourcePaths,
        private val startupConfig: StartupConfig
) {
    private val logger = Logger.getLogger(this.javaClass.name)


    /**
     * Start the configuration-runner
     *
     * @param config Received inofficial k6-configuration
     */
    public fun start(config: CtkConfiguration) {
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

        /*var baseURL = config.baseURL
        val localhostMatcher = "localhost|127\\.0\\.0\\.1".toRegex()
        //If config-runner runs inside docker, localhost can´t be used so its replaced by the alternative host
        if (alternativeTargetHost.isNotBlank() && localhostMatcher.containsMatchIn(config.baseURL)) {
            baseURL = config.baseURL.replace(localhostMatcher, alternativeTargetHost)
            logger.info("Alternative host was provided (\$dqualizer.dqexec.docker.localhost_replacement): Replacing 'localhost' or '127.0.0.1' in ${config.baseURL} with $alternativeTargetHost. Result: $baseURL")
        }*/

        var testCounter = 1
        val objectMapper = ObjectMapper()
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT)

        for (chaosExperiment in config.ctkChaosExperiments) {
            val experimentJsonPayload = objectMapper.writeValueAsString(chaosExperiment)

            logger.info("### CHAOS EXPERIMENT $testCounter WAS CREATED ###")
            var runCounter = 1

            while (runCounter <= chaosExperiment.repetitions){
                val exitValue = requestExperimentExecutionOnHost(experimentJsonPayload, testCounter, runCounter)
                logger.info(" CHAOS EXPERIMENT $testCounter-$runCounter FINISHED WITH VALUE $exitValue ###")
                runCounter++
            }
            testCounter++
        }
        logger.info("### RESILIENCE TESTING COMPLETE ###")
    }

    fun saveJsonToFile(jsonPayload: String, filePath: Path) {
        try {
            val file = filePath.toFile()
            // Create the parent directories if they don't exist
            file.parentFile.mkdirs()
            file.writeText(jsonPayload)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    // TODO make private again later
    @Throws(IOException::class, InterruptedException::class)
    public fun runExperimentOnLocalWindowsChaosToolkit(experimentPath: Path, testCounter: Int, runCounter: Int): Int {

        val activateVenvCommand = """C:\Users\HenningMöllers\IdeaProjects\Dqualizer_Resilienz_Manuelle_Uebersetzung\venv\Scripts\activate.ps1"""

        val executeExperimentCommand = """chaos run $experimentPath"""
        val activateAndExecuteCommand = "$activateVenvCommand ; $executeExperimentCommand"

      /*  println(activateVenvCommand)
        println(executeExperimentCommand)
        println(activateAndExecuteCommand)*/

        logger.info(
                """
            ### RUN COMMAND: $activateAndExecuteCommand ###
            """.trimIndent()
        )

        val process = ProcessBuilder("""C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe""",
                "-Command",
                activateAndExecuteCommand).inheritIO().start()

        val loggingPath = paths.getLogFilePath(testCounter, runCounter)
        processLogger.log(process, loggingPath)

        // Capture and print the output and error streams
       /* val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            println(line)
        }*/

        val exitCode = process.waitFor()
        println("Exit Code: $exitCode")
        return exitCode
    }

    // TODO make private again later
    @Throws(IOException::class, InterruptedException::class)
    fun requestExperimentExecutionOnHost(experimentJson: String, testCounter: Int, runCounter: Int): Int {

        val restTemplate = RestTemplate()
        val isRunningInDocker = Path("/proc/1/cgroup").exists()
        var url = ""
        // TODO make Url/Port configurable
        if (isRunningInDocker){
            url = "http://host.docker.internal:3323/execute_experiment"
        }
        else{
            url = "http://localhost:3323/execute_experiment"
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
