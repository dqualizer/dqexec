package dqualizer.dqexec.instrumentation.framework.included

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JavaLocationTest {
    @Test
    fun canProcessCorrectInputWithoutParameters() {
        val loc = JavaLocation.fromString("derp.nonexisting.com#IAmAMethodName")
        assertEquals("derp.nonexisting.com", loc.classIdentifier)
        assertEquals("IAmAMethodName", loc.methodName)
        assertEquals(false, loc.methodParameters.isPresent)
    }

    @Test
    fun canProcessCorrectInputWithParameters() {
        val loc = JavaLocation.fromString("derp.nonexisting.com#IAmAMethodName(String, Integer, float)")
        assertEquals("derp.nonexisting.com", loc.classIdentifier)
        assertEquals("IAmAMethodName", loc.methodName)
        assertEquals(true, loc.methodParameters.isPresent)
        assertEquals("(String, Integer, float)", loc.methodParameters.get())
    }
}
