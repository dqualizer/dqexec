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
      LoadTypes.LOW.type -> loadType.low

      LoadTypes.MEDIUM.type -> loadType.medium

      LoadTypes.HIGH.type -> loadType.high

      LoadTypes.VERY_HIGH.type -> loadType.veryHigh

      LoadTypes.EXTREMELY_HIGH.type -> loadType.extremelyHigh

      DurationTypes.SLOW.type -> durationType.slow

      DurationTypes.FAST.type -> durationType.fast

      DurationTypes.VERY_FAST.type -> durationType.veryFast

      else -> throw RuntimeException(valueName)
    }
  }

  private fun calculateDoubleValue(doubleValue: SymbolicDoubleValue): Double {
    val loadType = loadTestConstants.symbolics.load.decimal
    val durationType = loadTestConstants.symbolics.duration.decimal

    return when (val valueName = doubleValue.name) {
      LoadTypes.LOW.type -> loadType.low

      LoadTypes.MEDIUM.type -> loadType.medium

      LoadTypes.HIGH.type -> loadType.high

      LoadTypes.VERY_HIGH.type -> loadType.veryHigh

      LoadTypes.EXTREMELY_HIGH.type -> loadType.extremelyHigh

      DurationTypes.SLOW.type -> durationType.slow

      DurationTypes.FAST.type -> durationType.fast

      DurationTypes.VERY_FAST.type -> durationType.veryFast

      else -> throw RuntimeException(valueName)
    }
  }

  enum class TimeUnitType {
    LOAD,
    DURATION
  }

  private enum class LoadTypes(val type: String) {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    VERY_HIGH("VERY HIGH"),
    EXTREMELY_HIGH("EXTREMELY HIGH"),
  }

  private enum class DurationTypes(val type: String) {
    SLOW("SLOW"),
    FAST("FAST"),
    VERY_FAST("VERY FAST")
  }
}
