package dqualizer.dqexec.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class LoadCurveHelperTest {
    @Test
    fun testTrivialLoadCurve() {
        val computer = LoadCurveHelper(2.0, 0.0, 9.0, 3.0)
        Assertions.assertEquals(0.0, computer.evaluate(0.0))
        Assertions.assertEquals(1.0, computer.evaluate(1.0))
        Assertions.assertEquals(4.0, computer.evaluate(2.0))
        Assertions.assertEquals(9.0, computer.evaluate(3.0))
    }

    @Test
    fun testShiftedTrivialLoadCurve() {
        val computer = LoadCurveHelper(2.0, 1.0, 16.0, 3.0)
        Assertions.assertEquals(1.0, computer.evaluate(0.0))
        Assertions.assertEquals(4.0, computer.evaluate(1.0))
        Assertions.assertEquals(9.0, computer.evaluate(2.0))
        Assertions.assertEquals(16.0, computer.evaluate(3.0))
    }

    @Test
    fun testSqueezedLoadCurve() {
        val computer = LoadCurveHelper(2.0, 0.0, 16.0, 2.0)
        Assertions.assertEquals(0.0, computer.evaluate(0.0))
        Assertions.assertEquals(4.0, computer.evaluate(1.0))
        Assertions.assertEquals(16.0, computer.evaluate(2.0))
    }
}