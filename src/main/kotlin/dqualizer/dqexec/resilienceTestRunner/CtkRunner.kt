package dqualizer.dqexec.resilienceTestRunner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqexec.exception.RunnerFailedException
import dqualizer.dqexec.util.ProcessLogger
import io.github.dqualizer.dqlang.types.adapter.ctk.CtkConfiguration
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.Path

/**
 *
 */
@Service
class CtkRunner(
        private val processLogger: ProcessLogger,
        private val paths: ResourcePaths
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
            val jsonPayload = objectMapper.writeValueAsString(chaosExperiment)
            //val experimentFilePath = Path("C:/Users/HenningMöllers/IdeaProjects/dqexec/src/main/resources/ctk/generatedExperiments/${chaosExperiment.title.replace(" ", "")}_experiment.json")
            val experimentFilePath = Path("/app/ctk/generatedExperiments/${chaosExperiment.title.replace(" ", "")}_experiment.json")
            saveJsonToFile(jsonPayload, experimentFilePath)
            logger.info("### CHAOS EXPERIMENT $testCounter WAS CREATED IN $experimentFilePath ###")
            var runCounter = 1

            //repeat one loadtest if as many times as specified in the configuration
            while (runCounter <= chaosExperiment.repetitions){
                //val exitValue = runExperimentOnLocalWindowsChaosToolkit(experimentFilePath, testCounter, runCounter)
                val exitValue = runExperiment(experimentFilePath, testCounter, runCounter)
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
    public fun runExperiment(experimentPath: Path, testCounter: Int, runCounter: Int): Int {

        val executeExperimentCommand = """chaos run $experimentPath"""

        logger.info(
                """
            ### RUN COMMAND: $executeExperimentCommand ###
            """.trimIndent()
        )

        val process = ProcessBuilder(executeExperimentCommand).inheritIO().start()

        val loggingPath = paths.getLogFilePath(testCounter, runCounter)
        processLogger.log(process, loggingPath)

        // TODO Is already logged by processLogger?!
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
}
