package dqualizer.dqexec.util

import kotlin.math.pow

/**
 * A helper class that calculates points on a load curve of the format y=t^e with e being the exponent.
 * @param exponent the exponent of the base function
 * @param ymin     the minimum y value
 * @param ymax     the maxmium y value
 * @param targetDuration the duration of the load profile.
 */
class LoadCurveHelper(exponent: Double, ymin: Double, ymax: Double, targetDuration: Double) {
    private val exponent: Double
    private val startOnBaseFunction: Double
    private val baseFunctionDuration: Double
    private val targetDuration: Double

    init {
        require(exponent >= 1) { "Exponent needs to be at least 1, but was: $exponent" }
        require(ymin >= 0) { "Minimum y needs to be at least 0, but was: $ymin" }
        require(ymax >= ymin) { "Maximum y must be bigger than Minimum y ($ymin), but was: $ymax" }
        require(targetDuration > 0) { "Target Duration needs to be bigger than 0, but was: $exponent" }
        this.exponent = exponent
        startOnBaseFunction = ymin.pow(1.0 / exponent)
        val endOnBaseFunction = ymax.pow(1.0 / exponent)
        baseFunctionDuration = endOnBaseFunction - startOnBaseFunction
        this.targetDuration = targetDuration
    }

    /**
     * Returns y of the base function y=t^e for a given time t.
     *
     * @param time a point in time
     * @return y
     */
    private fun evaluateBaseFunction(time: Double): Double {
        return time.pow(exponent)
    }

    /**
     * Returns the value for a given point in time for a load profile of given length "duration" and with the form
     * y=t^e with e being an exponent defined at initalization. At the beginning (time = 0) the value is ymin and at
     * the end (time = duration) the value is ymax.
     *
     * @param time     the time, a value between 0 and duration
     * @return a value between ymin and ymax as defined at initialization.
     */
    fun evaluate(time: Double): Double {
        val mappedTime = startOnBaseFunction + baseFunctionDuration / targetDuration * time
        return evaluateBaseFunction(mappedTime)
    }
}