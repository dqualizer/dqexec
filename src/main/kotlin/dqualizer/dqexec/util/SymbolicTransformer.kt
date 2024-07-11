package dqualizer.dqexec.util

import io.github.dqualizer.dqlang.types.adapter.constants.LoadTestConstants
import io.github.dqualizer.dqlang.types.rqa.definition.stimulus.symbolic.SymbolicDoubleValue
import io.github.dqualizer.dqlang.types.rqa.definition.stimulus.symbolic.SymbolicIntValue
import io.github.dqualizer.dqlang.types.rqa.definition.stimulus.symbolic.SymbolicValue
import org.springframework.stereotype.Component
import java.time.Duration

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
        val timeUnit = loadTestConstants.symbolics.load.timeUnit
        // convert value to SECONDS
        val convertedValue = timeUnit.convert(Duration.ofSeconds(longValue))
        if(convertedValue <= 0) throw IllegalStateException("Too small load value")
        else convertedValue
      }

      TimeUnitType.DURATION -> {
        val timeUnit = loadTestConstants.symbolics.duration.timeUnit
        // k6 default unit is millisecond
        timeUnit.toMillis(longValue)
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
