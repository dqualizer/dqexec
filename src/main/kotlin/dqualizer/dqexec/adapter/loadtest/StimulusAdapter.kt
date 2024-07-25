package dqualizer.dqexec.adapter.loadtest

import dqualizer.dqexec.exception.UnknownTermException
import dqualizer.dqexec.util.LoadCurveHelper
import dqualizer.dqexec.util.SymbolicTransformer
import dqualizer.dqexec.util.SymbolicTransformer.TimeUnitType
import io.github.dqualizer.dqlang.types.adapter.constants.loadTest.LoadTestConstants
import io.github.dqualizer.dqlang.types.adapter.k6.options.*
import io.github.dqualizer.dqlang.types.rqa.definition.loadtest.stimulus.Stimulus
import io.github.dqualizer.dqlang.types.rqa.definition.loadtest.stimulus.loadprofile.ConstantLoad
import io.github.dqualizer.dqlang.types.rqa.definition.loadtest.stimulus.loadprofile.LoadIncrease
import io.github.dqualizer.dqlang.types.rqa.definition.loadtest.stimulus.loadprofile.LoadPeak
import org.springframework.stereotype.Component
import org.springframework.vault.support.DurationParser
import java.time.Duration
import kotlin.math.roundToInt

/** Adapts the stimulus to a k6 'options' object */
@Component
class StimulusAdapter(
  private val loadTestConstants: LoadTestConstants,
  private val symbolicTransformer: SymbolicTransformer
) {

  /**
   * Create a k6 'options' objects based on the stimulus for the loadtest
   *
   * @param stimulus Stimulus for the loadtest
   * @return A k6 'options' object
   */
  fun adaptStimulus(stimulus: Stimulus): Options {
    val loadProfile = stimulus.workload!!.loadProfile!!
    val accuracy = stimulus.accuracy!!
    val scenario: K6Scenario
    val scenarios: Scenarios

    when (loadProfile) {
      is LoadPeak -> {
        scenario = getLoadPeakScenario(loadProfile)
        scenarios = Scenarios(scenario)
      }

      is LoadIncrease -> {
        scenario = getLoadIncreaseScenario(loadProfile)
        scenarios = Scenarios(scenario)
      }

      is ConstantLoad -> {
        scenario = getConstantLoadScenario(loadProfile, accuracy)
        scenarios = Scenarios(scenario)
      }

      else -> {
        throw UnknownTermException(stimulus::class.java.name)
      }
    }
    return Options(scenarios)
  }

  /**
   * Create a k6 'scenario' object for the load_profile 'LOAD_PEAK'
   *
   * @param stimulus Stimulus for the loadtest
   * @return A k6 'scenario' object with virtual user ramp-up
   */
  fun getLoadIncreaseScenario(loadIncrease: LoadIncrease): K6Scenario {
    val highestLoad = symbolicTransformer.calculateValue(loadIncrease.highestLoad!!)
    val timeToHighestLoad = symbolicTransformer.calculateValue(loadIncrease.timeToHighestLoad!!)
    val constantDuration = symbolicTransformer.calculateValue(loadIncrease.constantDuration!!)

    val adaptedHighestLoad = symbolicTransformer.calculateTimeUnit(highestLoad, TimeUnitType.LOAD).toInt()
    val adaptedDuration = symbolicTransformer.calculateTimeUnit(timeToHighestLoad, TimeUnitType.LOAD).toString()
    val adaptedConstantDuration =
      symbolicTransformer.calculateTimeUnit(constantDuration, TimeUnitType.DURATION).toString()
    val coolDownDuration = loadTestConstants.technicalConstants.coolDownDuration.toString()

    val stage1 = Stage(adaptedDuration, adaptedHighestLoad)
    val stage2 = Stage(adaptedConstantDuration, adaptedHighestLoad)
    val stage3 = Stage(coolDownDuration, 0)

    val stages = LinkedHashSet<Stage>()
    stages.add(stage1)
    stages.add(stage2)
    stages.add(stage3)
    return RampingK6Scenario(stages = stages)
  }


  /**
   * Create a k6 'scenario' object for the load_profile 'LOAD_INCREASE'
   *
   * @param stimulus Stimulus for the loadtest
   * @return A k6 'scenario' object with increasing virtual user ramp-up
   */
  fun getLoadPeakScenario(loadPeak: LoadPeak): K6Scenario {
    val highestLoad = symbolicTransformer.calculateValue(loadPeak.peakLoad!!)
    val duration = symbolicTransformer.calculateValue(loadPeak.duration!!)

    val adaptedHighestLoad = symbolicTransformer.calculateTimeUnit(highestLoad, TimeUnitType.LOAD).toInt()
    val adaptedDuration = symbolicTransformer.calculateTimeUnit(duration, TimeUnitType.DURATION).toString()
    val coolDownDuration = loadTestConstants.technicalConstants.coolDownDuration.toString()

    val stage1 = Stage(adaptedDuration, adaptedHighestLoad)
    val stage2 = Stage(coolDownDuration, 0)

    val stages = LinkedHashSet<Stage>()
    stages.add(stage1)
    stages.add(stage2)

    return RampingK6Scenario(stages = stages)
  }

  /**
   * Create an ordered set of stages for the 'INCREASE_LOAD' load_profile
   *
   * @param startTarget The amount of users at the first stage
   * @param endTarget The amount of users at the last stage
   * @param exponent How fast should the amount of users increase
   * @return An ordered set of stages
   */
  private fun getPeakStages(
    startTarget: Int, endTarget: Int, exponent: Int, testDurationString: String,
    numberOfStages: Int
  ): LinkedHashSet<Stage> {
    val stages = LinkedHashSet<Stage>()
    val stageDurationString: String = computeStageDurationString(testDurationString, numberOfStages)
    val loadCurve =
      LoadCurveHelper(
        exponent.toDouble(),
        startTarget.toDouble(),
        endTarget.toDouble(),
        numberOfStages.toDouble(),
      )

    val firstStage = Stage("0s", startTarget)
    stages.add(firstStage)

    for (i in 0 until numberOfStages) {
      val endTimeOfStage = i + 1
      val currentStage =
        Stage(
          stageDurationString,
          loadCurve.evaluate(endTimeOfStage.toDouble()).roundToInt(),
        )
      stages.add(currentStage)
    }

    val lastStage = Stage(stageDurationString, 0)
    stages.add(lastStage)
    return stages
  }

  private fun computeStageDurationString(testDurationString: String, numberOfStages: Int): String {
    val testDuration = convertToTimeString(testDurationString)
    val testDurationSeconds = testDuration.seconds
    val testDurationNanos = testDuration.nano

    val stageDurationSeconds = testDurationSeconds / numberOfStages
    val stageDurationNanos = (testDurationNanos / numberOfStages).toLong()
    val stageDuration = Duration.ofSeconds(stageDurationSeconds).plusNanos(stageDurationNanos)

    return convertToTimeString(stageDuration)
  }

  /**
   * Create a k6 'scenario' object for the load_profile 'CONSTANT_LOAD'
   *
   * @param constantLoad Configured load profile
   * @param accuracy Accuracy of the load test
   * @return A k6 'scenario' object with constant virtual users
   */
  fun getConstantLoadScenario(constantLoad: ConstantLoad, accuracy: Int): K6Scenario {
    val targetLoad = symbolicTransformer.calculateValue(constantLoad.targetLoad!!)
    val duration = symbolicTransformer.calculateValue(constantLoad.duration!!)

    val adaptedTargetLoad = symbolicTransformer.calculateTimeUnit(targetLoad, TimeUnitType.LOAD).toInt()
    val adaptedDuration = symbolicTransformer.calculateTimeUnit(duration, TimeUnitType.DURATION).toString()

    //TODO Use accuracy

    return ConstantK6Scenario(vus = adaptedTargetLoad, duration = adaptedDuration)
  }

  private fun convertToTimeString(timeString: String): Duration {
    return requireNotNull(DurationParser.parseDuration(timeString))
  }

  private fun convertToTimeString(duration: Duration): String {
    return DurationParser.formatDuration(duration)
  }
}
