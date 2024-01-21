package dqualizer.dqexec.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LoadCurveHelperTest {
  @Test
  fun testTrivialLoadCurve() {
    val computer = LoadCurveHelper(2.0, 0.0, 9.0, 3.0)
    assertEquals(0.0, computer.evaluate(0.0))
    assertEquals(1.0, computer.evaluate(1.0))
    assertEquals(4.0, computer.evaluate(2.0))
    assertEquals(9.0, computer.evaluate(3.0))
  }

  @Test
  fun testShiftedTrivialLoadCurve() {
    val computer = LoadCurveHelper(2.0, 1.0, 16.0, 3.0)
    assertEquals(1.0, computer.evaluate(0.0))
    assertEquals(4.0, computer.evaluate(1.0))
    assertEquals(9.0, computer.evaluate(2.0))
    assertEquals(16.0, computer.evaluate(3.0))
  }

  @Test
  fun testSqueezedLoadCurve() {
    val computer = LoadCurveHelper(2.0, 0.0, 16.0, 2.0)
    assertEquals(0.0, computer.evaluate(0.0))
    assertEquals(4.0, computer.evaluate(1.0))
    assertEquals(16.0, computer.evaluate(2.0))
  }

  @Test
  fun throwsIllegalArgumentExceptionOnExponentSmallerThan1() {
    assertThrows<IllegalArgumentException> { LoadCurveHelper(0.0, 0.0, 1.0, 1.0) }
    assertThrows<IllegalArgumentException> { LoadCurveHelper(-1.0, 0.0, 1.0, 1.0) }
  }

  @Test
  fun throwsIllegalArgumentExceptionOnYMinLargerYMax() {
    assertThrows<IllegalArgumentException> { LoadCurveHelper(2.0, 1.0, 0.0, 3.0) }
  }

  @Test
  fun throwsIllegalArgumentExceptionOnNegativeDuration() {
    assertThrows<IllegalArgumentException> { LoadCurveHelper(2.0, 1.0, 0.0, -3.0) }
  }
}
