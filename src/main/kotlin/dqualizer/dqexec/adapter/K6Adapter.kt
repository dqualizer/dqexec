package dqualizer.dqexec.adapter

import dqualizer.dqexec.input.ConstantsLoader
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.LoadTestConstants
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.K6Config
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.K6LoadTest
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.LoadTestConfig
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.Stimulus
import org.springframework.stereotype.Component

/**
 * Adapts a loadtest configuration to an inoffical k6-configuration
 */
@Component
class K6Adapter(
    private val constantsLoader: ConstantsLoader,
    private val endpointAdapter: EndpointAdapter,
    private val stimulusAdapter: StimulusAdapter,
    private val loadtestConstants: LoadTestConstants
) {

    /**
     * Adapt the loadtest configuration. It consists of 3 steps:
     * 1. Adapt global test configuration
     * 2. Adapt the stimulus for every loadtest
     * 3. Adapt the endpoints for every loadtest
     * @param loadTestConfig The received dqlang load test configuration
     * @return An adapted loadtest configuration for k6
     */
    fun adapt(loadTestConfig: LoadTestConfig): K6Config {
        val name = loadTestConfig.context
        val baseURL = loadTestConfig.baseURL
        val loadTests =
            loadTestConfig.loadTests
        val k6LoadTests = LinkedHashSet<K6LoadTest>()
        for (loadTest in loadTests) {
            val stimulus = loadTest.stimulus
            val repetition = calculateRepetition(stimulus)
            val options =
                stimulusAdapter.adaptStimulus(stimulus)
            val endpoint = loadTest.endpoint
            val responseMeasure =
                loadTest.responseMeasure
            val request =
                endpointAdapter.adaptEndpoint(endpoint, responseMeasure)
            val k6LoadTest = K6LoadTest(repetition, options, request)
            k6LoadTests.add(k6LoadTest)
        }
        return K6Config(name, baseURL, k6LoadTests)
    }

    /**
     * Calculate how many times a loadtest should be repeated based on the defined accuracy
     * @param stimulus Stimulus for one loadtest
     * @return The amount of repetitions
     */
    private fun calculateRepetition(stimulus: Stimulus): Int {
        val loadProfile = stimulus.loadProfile
        if (loadProfile == "CONSTANT_LOAD") return 1
        val repetitionConstants = loadtestConstants.accuracy.repetition
        val accuracy = stimulus.accuracy
        val max = repetitionConstants.max
        val min = repetitionConstants.min
        val repetition = (max * (accuracy / 100.0)).toInt()
        return Math.max(repetition, min)
    }
}