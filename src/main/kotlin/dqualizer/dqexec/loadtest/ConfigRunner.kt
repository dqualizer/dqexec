package dqualizer.dqexec.loadtest

import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.config.K6ExecutorConfiguration
import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqexec.exception.RunnerFailedException
import dqualizer.dqexec.loadtest.mapper.k6.ScriptMapper
import dqualizer.dqexec.util.ProcessLogger
import io.github.dqualizer.dqlang.types.adapter.k6.K6Configuration
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.Path

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
    @Value("\${dqualizer.dqexec.docker.localhost_replacement:}") private val alternativeTargetHost: String,
    @Value("\${dqualizer.dqexec.influx.host:localhost}") private val influxHost: String,
) {
    private val logger = Logger.getLogger(this.javaClass.name)


    /**
     * Start the configuration-runner
     *
     * @param config Received inofficial k6-configuration
     */
    fun start(config: K6Configuration) {
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
    private fun run(config: K6Configuration) {
        logger.info("Trying to run configuration: " + ObjectMapper().writeValueAsString(config))

        var baseURL = config.baseURL
        val localhostMatcher = "localhost|127\\.0\\.0\\.1".toRegex()
        //If config-runner runs inside docker, localhost can´t be used so its replaced by the alternative host
        if (alternativeTargetHost.isNotBlank() && localhostMatcher.containsMatchIn(config.baseURL)) {
            baseURL = config.baseURL.replace(localhostMatcher, alternativeTargetHost)
            logger.info("Alternative host was provided (\$dqualizer.dqexec.docker.localhost_replacement): Replacing 'localhost' or '127.0.0.1' in ${config.baseURL} with $alternativeTargetHost. Result: $baseURL")
        }

        val loadTests = config.k6LoadTests
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
        val k6Path = "C:\\Users\\HenningMöllers\\Masterarbeit\\k6\\k6.exe"
        val command = "$k6Path run $scriptPath --out xk6-influxdb=http://$influxHost:8086"

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
        val exitCode = process.waitFor()
        println("Exit Code: $exitCode")
        return exitCode
    }
}
