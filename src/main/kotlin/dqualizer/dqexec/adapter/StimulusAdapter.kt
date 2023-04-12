package dqualizer.dqexec.adapter

import dqualizer.dqexec.exception.UnknownTermException
import dqualizer.dqexec.input.ConstantsLoader
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.LoadTestConstants
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.loadprofile.ConstantLoad
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.loadprofile.LoadIncrease
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.loadprofile.LoadPeak
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.Stage
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.options.*
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.Stimulus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.util.LinkedHashSet

/**
 * Adapts the stimulus to a k6 'options' object
 */
@Component
class StimulusAdapter {

    @Autowired
    private val constantsLoader: ConstantsLoader? = null

    /**
     * Create a k6 'options' objects based on the stimulus for the loadtest
     *
     * @param stimulus Stimulus for the loadtest
     * @return A k6 'options' object
     */
    fun adaptStimulus(stimulus: Stimulus): Options {
        val loadProfile = stimulus.loadProfile
        val scenario: Scenario

        when (LoadProfileType.valueOf(loadProfile)) {
            LoadProfileType.LOAD_PEAK -> {
                scenario = getLoadPeakScenario(stimulus)
            }
            LoadProfileType.LOAD_INCREASE -> {
                scenario = getLoadIncreaseScenario(stimulus)
            }
            LoadProfileType.CONSTANT_LOAD -> {
                scenario = getConstantLoadScenario(stimulus)
            }
            else -> {
                throw UnknownTermException("Unknown load profile type: $loadProfile")
            }
        }
        val scenarios = Scenarios(scenario)

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
    fun getLoadPeakScenario(stimulus: Stimulus): Scenario {
        val constants = constantsLoader!!.load()
        val loadPeak = constants.loadProfile.loadPeak

        val highestLoad = stimulus.highestLoad
        val target: Int
        target = when (PeakHeight.valueOf(highestLoad)) {
            PeakHeight.HIGH -> {
                loadPeak.high
            }
            PeakHeight.VERY_HIGH -> {
                loadPeak.veryHigh
            }
            PeakHeight.EXTREMELY_HIGH -> {
                loadPeak.extremelyHigh
            }
            else -> {
                throw UnknownTermException("Unknown peak height: $highestLoad")
            }
        }

        val timeToHighestLoad = stimulus.timeToHighestLoad
        val duration: String
        when (TimeToHighestLoad.valueOf(timeToHighestLoad)) {
            TimeToHighestLoad.SLOW -> {
                duration = loadPeak.slow
            }
            TimeToHighestLoad.FAST -> {
                duration = loadPeak.fast
            }
            TimeToHighestLoad.VERY_FAST -> {
                duration = loadPeak.veryFast
            }
            else -> {
                throw UnknownTermException("Unknown time to highest load: $timeToHighestLoad")
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
    fun getLoadIncreaseScenario(stimulus: Stimulus): Scenario {
        val constants = constantsLoader!!.load()
        val loadIncrease = constants.loadProfile.loadIncrease

        val typeOfIncrease = stimulus.typeOfIncrease
        val exponent: Int
        when (TypeOfIncrease.valueOf(typeOfIncrease)) {
            TypeOfIncrease.CUBIC -> {
                exponent = loadIncrease.cubic
            }
            TypeOfIncrease.QUADRATIC -> {
                exponent = loadIncrease.quadratic
            }
            TypeOfIncrease.LINEAR -> {
                exponent = loadIncrease.linear
            }
            else -> {
                throw UnknownTermException("Unknown type of increase: $typeOfIncrease")
            }
        }

        val endTarget = loadIncrease.endTarget
        val startTarget = loadIncrease.startTarget
        val stageDuration = loadIncrease.stageDuration

        val stages = this.getIncreasingStages(stageDuration, startTarget, endTarget, exponent)
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
     * @param stageDuration The duration of one single stage
     * @param startTarget   The amount of users at the first stage
     * @param endTarget     The amount of users at the last stage
     * @param exponent      How fast should the amount of users increase
     * @return An ordered set of stages
     */
    private fun getIncreasingStages(
        stageDuration: String,
        startTarget: Int,
        endTarget: Int,
        exponent: Int
    ): LinkedHashSet<Stage> {
        val stages = LinkedHashSet<Stage>()
        var stageCounter = 1
        var currentTarget = startTarget

        while (currentTarget < endTarget) {
            currentTarget = startTarget * Math.pow(stageCounter.toDouble(), exponent.toDouble()).toInt()
            val currentStage = Stage(stageDuration, currentTarget)
            stages.add(currentStage)

            stageCounter++
        }
        val lastStage = Stage(stageDuration, 0)
        stages.add(lastStage)

        return stages
    }

    /**
     * Create a k6 'scenario' object for the load_profile 'CONSTANT_LOAD'
     *
     * @param stimulus Stimulus for the loadtest
     * @return A k6 'scenario' object with constant virtual users
     */
    fun getConstantLoadScenario(stimulus: Stimulus): Scenario {
        val constants = constantsLoader!!.load()
        val constantLoad = constants.loadProfile.constantLoad

        val baseLoad = stimulus.baseLoad
        val vus: Int
        when (BaseLoad.valueOf(baseLoad)) {
            BaseLoad.LOW -> {
                vus = constantLoad.low
            }
            BaseLoad.MEDIUM -> {
                vus = constantLoad.medium
            }
            BaseLoad.HIGH -> {
                vus = constantLoad.high
            }
            else -> {
                throw UnknownTermException("Unknown base load: $baseLoad")
            }
        }

        val accuracy = stimulus.accuracy
        val maxDuration = constantLoad.maxDuration
        val minDuration = constantLoad.minDuration

        val duration = (maxDuration * (accuracy / 100.0)).toInt()
        val trueDuration = Math.max(minDuration, duration)

        return ConstantScenario(vus, trueDuration)
    }

    private enum class BaseLoad {
        LOW,
        MEDIUM,
        HIGH
    }
}