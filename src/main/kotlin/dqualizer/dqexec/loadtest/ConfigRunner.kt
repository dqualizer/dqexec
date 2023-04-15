package dqualizer.dqexec.loadtest

import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.config.K6ExecutorConfiguration
import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqexec.exception.RunnerFailedException
import dqualizer.dqexec.loadtest.mapper.k6.ScriptMapper
import dqualizer.dqexec.util.ProcessLogger
import dqualizer.dqlang.archive.k6configurationrunner.dqlang.Config
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Path
import java.util.logging.Logger

/**
 * The execution of an inoffical k6 configuration consists of 4 steps:
 * 1. For every loadtest inside the configuration, create a k6-script (Javascript)
 * 2. Every script will be executed at least once or as many times as specified repetitions
 * 3. For every execution the k6 console log will be written to text file
 * 4. After one execution, the result metrics will be exported to InfluxDB
 */
@Service
class ConfigRunner(
    private val processLogger: ProcessLogger,
    private val writer: MultiLineFileWriter,
    private val mapper: ScriptMapper,
    private val paths: ResourcePaths,
    private val k6ExecutionConfiguration: K6ExecutorConfiguration,
    @Value("\${dqualizer.api.host:127.0.0.1}") private val APIHost: String,
    @Value("\${dqualizer.dqexec.influx.host:localhost}") private val influxHost: String,
) {
    private val logger = Logger.getLogger(this.javaClass.name)


    /**
     * Import the k6 configuration and start the configuration runner
     * @param config An inofficial k6 configuration
     */
    @RabbitListener(queues = ["\${dqualizer.rabbitmq.queues.k6}"])
    fun receive(@Payload config: Config) {
        logger.info("Received k6 configuration\n" + config)

//        val parsedAdaptedLoadTestConfig = objectMapper.readValue(config, Config::class.java)
        start(config)
    }


    /**
     * Start the configuration-runner
     *
     * @param config Received inofficial k6-configuration
     */
    private fun start(config: Config) {
        logger.info("### LOAD TEST CONFIGURATION RECEIVED ###")
        try {
            this.run(config)
        } catch (e: Exception) {
            logger.severe("### LOAD TESTING FAILED ###")
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
    private fun run(config: Config) {
        val om = ObjectMapper()
        logger.info(om.writeValueAsString(config))
        val localBaseURL = config.baseURL
        //If config-runner runs inside docker, localhost can´t be used
        val baseURL = localBaseURL.replace("127.0.0.1", APIHost)
        val loadTests = config.loadTests
        var testCounter = 1

        //iterate through all loadtests inside the configuration
        for (loadTest in loadTests) {
            val script = mapper.getScript(baseURL, loadTest)
            val scriptPath = paths.getScriptFilePath(testCounter)
            writer.write(script, scriptPath.toFile())
            logger.info("### SCRIPT $testCounter WAS CREATED ###")
            val repetition = loadTest.repetition
            var runCounter = 1

            //repeat one loadtest if as many times as specified in the configuration
            while (runCounter <= repetition) {
                val exitValue = runTest(scriptPath, testCounter, runCounter)
                logger.info("### LOAD TEST $testCounter-$runCounter FINISHED WITH VALUE $exitValue ###")
                runCounter++
            }
            testCounter++
        }
        logger.info("### LOAD TESTING COMPLETE ###")
    }

    /**
     * Run one k6-script, write the k6 console logs to text files and export the k6 result metrics to influxDB
     *
     * @param scriptPath  Location of the created k6-script
     * @param testCounter Current loadtest number
     * @param runCounter  Current repetition number
     * @return Exitcode of the k6 process
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun runTest(scriptPath: Path, testCounter: Int, runCounter: Int): Int {
        val command = "k6 -v run $scriptPath --out xk6-influxdb=http://$influxHost:8086"

        val currentEnv = System.getenv().toMutableMap()
        currentEnv["K6_INFLUXDB_ORGANIZATION"] = k6ExecutionConfiguration.influxdbOrganization
        currentEnv["K6_INFLUXDB_BUCKET"] = k6ExecutionConfiguration.influxdbBucket
        currentEnv["K6_INFLUXDB_TOKEN"] = k6ExecutionConfiguration.influxdbToken

        val envp = currentEnv.entries.map { it.key + "=" + it.value }.toTypedArray()

        logger.info(
            """### RUN COMMAND: $command ###
            |With Environment:
            |${envp.joinToString(separator = "\n")}                
        """.trimMargin()
        )

        val process = Runtime.getRuntime().exec(command, envp)
        val loggingPath = paths.getLogFilePath(testCounter, runCounter)
        processLogger.log(process, loggingPath.toFile())
        return process.exitValue()
    }
}
