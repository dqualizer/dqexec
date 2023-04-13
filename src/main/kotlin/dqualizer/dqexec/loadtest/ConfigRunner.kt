package dqualizer.dqexec.loadtest

import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.config.PathConfig
import dqualizer.dqexec.exception.RunnerFailedException
import dqualizer.dqexec.loadtest.mapper.k6.ScriptMapper
import dqualizer.dqlang.archive.k6configurationrunner.dqlang.Config
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component
import poc.util.HostRetriever
import poc.util.ProcessLogger
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
@Component
@RequiredArgsConstructor
class ConfigRunner {
    private val logger = Logger.getLogger(this.javaClass.name)
    private val paths: PathConfig? = null
    private val hostRetriever: HostRetriever? = null
    private val mapper: ScriptMapper? = null
    private val writer: ScriptWriter? = null
    private val processLogger: ProcessLogger? = null

    /**
     * Start the configuration-runner
     *
     * @param config Received inofficial k6-configuration
     */
    fun start(config: Config) {
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
        val baseURL = localBaseURL.replace("127.0.0.1", hostRetriever!!.aPIHost!!)
        val loadTests = config.loadTests
        var testCounter = 1

        //iterate through all loadtests inside the configuration
        for (loadTest in loadTests) {
            val script = mapper!!.getScript(baseURL, loadTest)
            val scriptPath = paths!!.getScriptFilePath(testCounter)
            writer!!.write(script, scriptPath)
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
        val influxHost = hostRetriever!!.influxHost
        val command = "k6 run $scriptPath --out xk6-influxdb=http://$influxHost:8086"
        val process = Runtime.getRuntime().exec(command)
        val loggingPath = paths!!.getLogFilePath(testCounter, runCounter)
        processLogger!!.log(process, loggingPath.toFile())
        return process.exitValue()
    }
}