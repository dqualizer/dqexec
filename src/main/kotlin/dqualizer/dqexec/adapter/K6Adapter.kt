package dqualizer.dqexec.adapter

import io.github.dqualizer.dqlang.types.adapter.constants.LoadTestConstants
import io.github.dqualizer.dqlang.types.adapter.k6.K6Configuration
import io.github.dqualizer.dqlang.types.adapter.k6.K6LoadTest
import io.github.dqualizer.dqlang.types.rqa.configuration.loadtest.LoadTestArtifact
import io.github.dqualizer.dqlang.types.rqa.configuration.loadtest.LoadTestConfiguration
import io.github.dqualizer.dqlang.types.rqa.definition.stimulus.Stimulus
import io.github.dqualizer.dqlang.types.rqa.definition.stimulus.loadprofile.ConstantLoad
import org.springframework.stereotype.Component

/** Adapts a loadtest configuration to an inoffical k6-configuration */
@Component
class K6Adapter(
  private val endpointAdapter: EndpointAdapter,
  private val stimulusAdapter: StimulusAdapter,
  private val loadtestConstants: LoadTestConstants,
) {
  /**
   * Adapt the loadtest configuration. It consists of 3 steps:
   * 1. Adapt global test configuration
   * 2. Adapt the stimulus for every loadtest
   * 3. Adapt the endpoints for every loadtest
   *
   * @param loadTestConfig The received dqlang load test configuration
   * @return An adapted loadtest configuration for k6
   */
  fun adapt(loadTestConfig: LoadTestConfiguration): K6Configuration {
    val name = loadTestConfig.context
    val baseURL = loadTestConfig.baseURL
    val loadTestArtifacts: Set<LoadTestArtifact> = loadTestConfig.loadTestArtifacts!!
    val k6LoadTests = LinkedHashSet<K6LoadTest>()
    for (loadTest in loadTestArtifacts) {
      val stimulus = loadTest.stimulus!!
      val repetition = calculateRepetition(stimulus)
      val options = stimulusAdapter.adaptStimulus(stimulus)
      val endpoint = loadTest.httpEndpoint!!
      val responseMeasure = loadTest.responseMeasure!!
      val request = endpointAdapter.adaptEndpoint(endpoint, responseMeasure)
      val k6LoadTest = K6LoadTest(repetition, options, request)
      k6LoadTests.add(k6LoadTest)
    }
    return K6Configuration(name, baseURL, k6LoadTests)
  }

  /**
   * Calculate how many times a loadtest should be repeated based on the defined accuracy
   *
   * @param stimulus Stimulus for one loadtest
   * @return The amount of repetitions
   */
  private fun calculateRepetition(stimulus: Stimulus): Int {
    // TODO Differentiate workload types
    val loadProfile = stimulus.workload!!.loadProfile!!
    if (loadProfile is ConstantLoad) return 1

    val repetitionConstants = loadtestConstants.accuracy.repetition!!
    val accuracy = stimulus.accuracy!!
    val max = repetitionConstants.max!!
    val min: Int = repetitionConstants.min!!
    val repetition: Int = (max * (accuracy / 100.0)).toInt()
    return repetition.coerceAtLeast(min)
  }
}

