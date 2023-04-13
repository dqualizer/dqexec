package dqualizer.dqexec.loadtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import dqualizer.dqexec.config.PathConfig;
import dqualizer.dqexec.loadtest.mapper.k6.ScriptMapper;
import dqualizer.dqlang.archive.k6configurationrunner.dqlang.Config;
import dqualizer.dqlang.archive.k6configurationrunner.dqlang.LoadTest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import dqualizer.dqexec.exception.RunnerFailedException;
import poc.util.HostRetriever;
import poc.util.ProcessLogger;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;

/**
 * The execution of an inoffical k6 configuration consists of 4 steps:
 * 1. For every loadtest inside the configuration, create a k6-script (Javascript)
 * 2. Every script will be executed at least once or as many times as specified repetitions
 * 3. For every execution the k6 console log will be written to text file
 * 4. After one execution, the result metrics will be exported to InfluxDB
 */
@Component
@RequiredArgsConstructor
public class ConfigRunner {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private final PathConfig paths;
    private final HostRetriever hostRetriever;
    private final ScriptMapper mapper;
    private final ScriptWriter writer;
    private final ProcessLogger processLogger;

    /**
     * Start the configuration-runner
     *
     * @param config Received inofficial k6-configuration
     */
    public void start(Config config) {
        logger.info("### LOAD TEST CONFIGURATION RECEIVED ###");
        try {
            this.run(config);
        } catch (Exception e) {
            logger.severe("### LOAD TESTING FAILED ###");
            e.printStackTrace();
            throw new RunnerFailedException(e.getMessage());
        }
    }

    /**
     * Read the configuration and start with the text execution
     *
     * @param config Received inofficial k6-configuration
     * @throws IOException
     * @throws InterruptedException
     */
    private void run(Config config) throws IOException, InterruptedException {
        ObjectMapper om = new ObjectMapper();
        logger.info(om.writeValueAsString(config));

        String localBaseURL = config.getBaseURL();
        //If config-runner runs inside docker, localhost can´t be used
        String baseURL = localBaseURL.replace("127.0.0.1", hostRetriever.getAPIHost());
        LinkedHashSet<LoadTest> loadTests = config.getLoadTests();
        int testCounter = 1;

        //iterate through all loadtests inside the configuration
        for (LoadTest loadTest : loadTests) {
            List<String> script = mapper.getScript(baseURL, loadTest);
            String scriptPath = paths.getScript(testCounter);
            writer.write(script, scriptPath);
            logger.info("### SCRIPT " + testCounter + " WAS CREATED ###");

            int repetition = loadTest.getRepetition();
            int runCounter = 1;

            //repeat one loadtest if as many times as specified in the configuration
            while (runCounter <= repetition) {
                int exitValue = this.runTest(scriptPath, testCounter, runCounter);
                logger.info("### LOAD TEST " + testCounter + "-" + runCounter + " FINISHED WITH VALUE " + exitValue + " ###");
                runCounter++;
            }
            testCounter++;
        }
        logger.info("### LOAD TESTING COMPLETE ###");
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
    private int runTest(String scriptPath, int testCounter, int runCounter) throws IOException, InterruptedException {
        String influxHost = hostRetriever.getInfluxHost();
        String command = "k6 run " + scriptPath + " --out xk6-influxdb=http://" + influxHost + ":8086";
        Process process = Runtime.getRuntime().exec(command);

        String loggingPath = paths.getLogging(testCounter, runCounter);
        processLogger.log(process, loggingPath);
        return process.exitValue();
    }
}