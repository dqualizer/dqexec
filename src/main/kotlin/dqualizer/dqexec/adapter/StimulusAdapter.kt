package dqualizer.dqexec.adapter

import dqualizer.dqexec.exception.UnknownStimulusException
import dqualizer.dqexec.util.LoadCurveHelper
import io.github.dqualizer.dqlang.types.adapter.constants.LoadTestConstants
import io.github.dqualizer.dqlang.types.adapter.k6.Stage
import io.github.dqualizer.dqlang.types.adapter.options.*
import io.github.dqualizer.dqlang.types.rqa.definition.stimulus.ConstantLoadStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.stimulus.LoadIncreaseStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.stimulus.LoadPeakStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.stimulus.Stimulus

import org.springframework.stereotype.Component
import org.springframework.vault.support.DurationParser
import java.time.Duration
import java.util.*
import java.util.logging.Logger
import kotlin.math.roundToInt

/**
 * Adapts the stimulus to a k6 'options' object
 */
@Component
class StimulusAdapter(private val loadTestConstants: LoadTestConstants) {
    private val log = Logger.getLogger(this.javaClass.name)

    /**
     * Create a k6 'options' objects based on the stimulus for the loadtest
     *
     * @param stimulus Stimulus for the loadtest
     * @return A k6 'options' object
     */
    fun adaptStimulus(stimulus: Stimulus): Options {
        val loadProfile = stimulus.loadProfile
        var scenario: Scenario
        var scenarios: Scenarios
        when(stimulus) {
            is LoadPeakStimulus -> {
                scenario = getLoadPeakScenario(stimulus)
                scenarios = Scenarios(scenario)
            }

            is LoadIncreaseStimulus -> {
                scenario = getLoadIncreaseScenario(stimulus)
                scenarios = Scenarios(scenario)
            }
            is ConstantLoadStimulus -> {
                scenario = getConstantLoadScenario(stimulus)
                scenarios = Scenarios(scenario)
            }
            else -> {
                throw UnknownStimulusException("Received unsupported stimulus type: ${stimulus::class.java.name}")
            }

        }
        return Options(scenarios)
    }

    private enum class LoadProfileType {
        LOAD_PEAK,
        LOAD_INCREASE,
        CONSTANT_LOAD
    }

    /**
     * Create a k6 'scenario' object for the load_profile 'LOAD_PEAK'
     *
     * @param stimulus Stimulus for the loadtest
     * @return A k6 'scenario' object with virtual user ramp-up
     */
    fun getLoadPeakScenario(stimulus: LoadPeakStimulus): Scenario {

        val loadPeak = loadTestConstants.loadProfile.loadPeak

        val highestLoad = stimulus.highestLoad
        val target: Int = when (PeakHeight.valueOf(highestLoad.toString())) {
            PeakHeight.HIGH -> {
                loadPeak.high
            }

            PeakHeight.VERY_HIGH -> {
                loadPeak.veryHigh
            }

            PeakHeight.EXTREMELY_HIGH -> {
                loadPeak.extremelyHigh
            }
        }

        val timeToHighestLoad = stimulus.timeToHighestLoad
        val duration: String = when (TimeToHighestLoad.valueOf(timeToHighestLoad.toString())) {
            TimeToHighestLoad.SLOW -> {
                loadPeak.slow
            }

            TimeToHighestLoad.FAST -> {
                loadPeak.fast
            }

            TimeToHighestLoad.VERY_FAST -> {
                loadPeak.veryFast
            }
        }
        val coolDownDuration = loadPeak.coolDownDuration

        val stage1 = Stage(duration, target)
        val stage2 = Stage(coolDownDuration, 0)

        val stages = LinkedHashSet<Stage>()
        stages.add(stage1)
        stages.add(stage2)
        return RampingScenario(stages)
    }

    private enum class PeakHeight {
        HIGH,
        VERY_HIGH,
        EXTREMELY_HIGH
    }

    private enum class TimeToHighestLoad {
        SLOW,
        FAST,
        VERY_FAST
    }

    /**
     * Create a k6 'scenario' object for the load_profile 'LOAD_INCREASE'
     *
     * @param stimulus Stimulus for the loadtest
     * @return A k6 'scenario' object with increasing virtual user ramp-up
     */
    fun getLoadIncreaseScenario(stimulus: LoadIncreaseStimulus): Scenario {
        val loadIncrease = loadTestConstants.loadProfile.loadIncrease

        val typeOfIncrease = stimulus.typeOfIncrease
        val exponent: Int = when (TypeOfIncrease.valueOf(typeOfIncrease.toString())) {
            TypeOfIncrease.CUBIC -> {
                loadIncrease.cubic
            }

            TypeOfIncrease.QUADRATIC -> {
                loadIncrease.quadratic
            }

            TypeOfIncrease.LINEAR -> {
                loadIncrease.linear
            }
        }

        val endTarget = loadIncrease.endTarget
        val startTarget = loadIncrease.startTarget
        val numberOfStages = loadIncrease.stages
        val testDuration: String = loadIncrease.testDuration

        val stages = getIncreasingStages(startTarget, endTarget, exponent, testDuration, numberOfStages)
        return RampingScenario(stages)
    }

    private enum class TypeOfIncrease {
        LINEAR,
        QUADRATIC,
        CUBIC
    }

    /**
     * Create an ordered set of stages for the 'INCREASE_LOAD' load_profile
     *
     * @param startTarget The amount of users at the first stage
     * @param endTarget   The amount of users at the last stage
     * @param exponent    How fast should the amount of users increase
     * @return An ordered set of stages
     */
    private fun getIncreasingStages(
        startTarget: Int, endTarget: Int, exponent: Int,
        testDurationString: String, numberOfStages: Int
    ): LinkedHashSet<Stage> {
        val stages = LinkedHashSet<Stage>()
        val stageDurationString: String = computeStageDurationString(testDurationString, numberOfStages)
        val loadCurve = LoadCurveHelper(exponent.toDouble(), startTarget.toDouble(), endTarget.toDouble(), numberOfStages.toDouble())
        val firstStage = Stage("0s", startTarget)
        stages.add(firstStage)
        for (i in 0 until numberOfStages) {
            val endTimeOfStage = i + 1
            val currentStage = Stage(
                stageDurationString,
                loadCurve.evaluate(endTimeOfStage.toDouble()).roundToInt()
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
     * @param stimulus Stimulus for the loadtest
     * @return A k6 'scenario' object with constant virtual users
     */
    fun getConstantLoadScenario(stimulus: ConstantLoadStimulus): Scenario {
        val constantLoad = loadTestConstants.loadProfile.constantLoad

        val baseLoad = stimulus.baseLoad
        val vus: Int = when (BaseLoad.valueOf(baseLoad.toString())) {
            BaseLoad.LOW -> {
                constantLoad.low
            }

            BaseLoad.MEDIUM -> {
                constantLoad.medium
            }

            BaseLoad.HIGH -> {
                constantLoad.high
            }
        }

        val accuracy = stimulus.accuracy
        val maxDuration = constantLoad.maxDuration
        val minDuration = constantLoad.minDuration

        val duration = (maxDuration * (accuracy / 100.0)).toInt()
        val trueDuration = minDuration.coerceAtLeast(duration)

        return ConstantScenario(vus, trueDuration)
    }

    private enum class BaseLoad {
        LOW,
        MEDIUM,
        HIGH
    }

    private fun convertToTimeString(timeString: String): Duration {
        return requireNotNull(DurationParser.parseDuration(timeString))
    }

    private fun convertToTimeString(duration: Duration): String {
        return DurationParser.formatDuration(duration)
    }
}