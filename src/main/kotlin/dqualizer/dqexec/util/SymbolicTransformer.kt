package dqualizer.dqexec.util

import io.github.dqualizer.dqlang.types.adapter.constants.LoadTestConstants
import io.github.dqualizer.dqlang.types.rqa.definition.stimulus.symbolic.SymbolicDoubleValue
import io.github.dqualizer.dqlang.types.rqa.definition.stimulus.symbolic.SymbolicIntValue
import io.github.dqualizer.dqlang.types.rqa.definition.stimulus.symbolic.SymbolicValue
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class SymbolicTransformer(private val loadTestConstants: LoadTestConstants) {

  fun calculateValue(symbolicValue: SymbolicValue): Number {
    return when (symbolicValue) {
      is SymbolicIntValue ->
        calculateIntValue(symbolicValue)

      is SymbolicDoubleValue ->
        calculateDoubleValue(symbolicValue)

      else ->
        throw RuntimeException(symbolicValue.javaClass.name)
    }
  }

  fun calculateTimeUnit(value: Number, type: TimeUnitType): Number {
    val longValue = value.toLong()
    return when (type) {
      TimeUnitType.LOAD -> {
        val timeUnit: TimeUnit = loadTestConstants.symbolics.load.timeUnit
        // Since load uses the timeUnit in the denominator (for example user/SECONDS),
        // the value has to be divided and not multiplied
        // Since toSeconds() always uses multiplication, the factor is extracted and after that used for division
        val newValue = timeUnit.toSeconds(longValue).toDouble()
        val multiplicationFactor = newValue / longValue
        longValue / multiplicationFactor
      }

      TimeUnitType.DURATION -> {
        val timeUnit: TimeUnit = loadTestConstants.symbolics.duration.timeUnit
        timeUnit.toSeconds(longValue)
      }
    }
  }

  private fun calculateIntValue(intValue: SymbolicIntValue): Int {
    val loadType = loadTestConstants.symbolics.load.integer
    val durationType = loadTestConstants.symbolics.duration.integer

    return when (val valueName = intValue.name) {
      LoadTypes.LOW.name -> loadType.low

      LoadTypes.MEDIUM.name -> loadType.medium

      LoadTypes.HIGH.name -> loadType.high

      DurationTypes.SLOW.name -> durationType.slow

      DurationTypes.FAST.name -> durationType.fast

      DurationTypes.VERY_FAST.name -> durationType.veryFast

      else -> throw RuntimeException(valueName)
    }
  }

  private fun calculateDoubleValue(doubleValue: SymbolicDoubleValue): Double {
    val loadType = loadTestConstants.symbolics.load.decimal
    val durationType = loadTestConstants.symbolics.duration.decimal

    return when (val valueName = doubleValue.name) {
      LoadTypes.LOW.name -> loadType.low

      LoadTypes.MEDIUM.name -> loadType.medium

      LoadTypes.HIGH.name -> loadType.high

      DurationTypes.SLOW.name -> durationType.slow

      DurationTypes.FAST.name -> durationType.fast

      DurationTypes.VERY_FAST.name -> durationType.veryFast

      else -> throw RuntimeException(valueName)
    }
  }

  enum class TimeUnitType {
    LOAD,
    DURATION
  }

  private enum class LoadTypes {
    LOW,
    MEDIUM,
    HIGH,
  }

  private enum class DurationTypes {
    SLOW,
    FAST,
    VERY_FAST
  }
}
