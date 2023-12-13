package dqualizer.dqexec.resilienceTestRunner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import dqualizer.dqexec.config.CtkExecutorConfiguration
import dqualizer.dqexec.exception.RunnerFailedException
import dqualizer.dqexec.util.ProcessLogger
import io.github.dqualizer.dqlang.types.adapter.ctk.CtkConfiguration
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.Path

/**
 *
 */
@Service
class ctkRunner(
        private val processLogger: ProcessLogger,
        private val ctkExecutorConfiguration: CtkExecutorConfiguration,
        @Value("\${dqualizer.dqexec.docker.localhost_replacement:}") private val alternativeTargetHost: String,
        @Value("\${dqualizer.dqexec.influx.host:localhost}") private val influxHost: String,
) {
    private val logger = Logger.getLogger(this.javaClass.name)


    /**
     * Import the k6 configuration and start the configuration runner
     * @param config An inofficial k6 configuration
     */
    @RabbitListener(queues = ["\${dqualizer.messaging.queues.ctk.name}"])
    fun receive(@Payload config: CtkConfiguration) {
        logger.info("Received CTK configuration\n" + config)
        start(config)
    }


    /**
     * Start the configuration-runner
     *
     * @param config Received inofficial k6-configuration
     */
    private fun start(config: CtkConfiguration) {
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
            val jsonPayload = objectMapper.writeValueAsString(chaosExperiment)
            val experimentFilePath = Path("resources/ctk/experiments/${chaosExperiment.title}_experiment.json")
            saveJsonToFile(jsonPayload, experimentFilePath)
            logger.info("### EXPERIMENT $testCounter WAS CREATED IN $experimentFilePath ###")
            var runCounter = 1

            //repeat one loadtest if as many times as specified in the configuration
            while (runCounter <= chaosExperiment.repitions) {
                val exitValue = runTest(experimentFilePath, testCounter, runCounter)
                logger.info("### Chaos Experiment $testCounter-$runCounter FINISHED WITH VALUE $exitValue ###")
                runCounter++
            }
            testCounter++
        }
        logger.info("### RESILIENCE TESTING COMPLETE ###")
    }

    fun saveJsonToFile(jsonPayload: String, filePath: Path) {
        try {
            val file = filePath.toFile()
            file.writeText(jsonPayload)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    @Throws(IOException::class, InterruptedException::class)
    private fun runTest(experimentPath: Path, testCounter: Int, runCounter: Int): Int {
        // TODO use trigger in docker container (makes mounting of filepath necessary...) OR trigger in local venv?!
        val command = "k6 run $experimentpath --out xk6-influxdb=http://$influxHost:8086"

        val envp = arrayOf(
            "K6_INFLUXDB_ORGANIZATION=${k6ExecutionConfiguration.influxdbOrganization}",
            "K6_INFLUXDB_BUCKET=${k6ExecutionConfiguration.influxdbBucket}",
            "K6_INFLUXDB_TOKEN=${k6ExecutionConfiguration.influxdbToken}"
        )

        logger.info(
            """
            ### RUN COMMAND: $command ###
            With Environment:
            ${envp.joinToString("\n")}                
            """.trimIndent()
        )

        val process = Runtime.getRuntime().exec(command, envp)
        val loggingPath = paths.getLogFilePath(testCounter, runCounter)
        processLogger.log(process, loggingPath)
        return process.exitValue()
    }
}
