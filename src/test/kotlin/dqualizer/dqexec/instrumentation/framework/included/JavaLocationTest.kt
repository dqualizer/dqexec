package dqualizer.dqexec.instrumentation.framework.included

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JavaLocationTest {
    @Test
    fun canProcessCorrectInputWithoutParameters() {
        val loc = JavaLocation.fromString("derp.nonexisting.com#IAmAMethodName")
        assertEquals("derp.nonexisting.com", loc.classIdentifier)
        assertEquals("IAmAMethodName", loc.methodName.get())
        assertEquals(false, loc.methodParameters.isPresent)
    }

    @Test
    fun canProcessCorrectInputWithParameters() {
        val loc = JavaLocation.fromString("derp.nonexisting.com#IAmAMethodName(String, Integer, float)")
        assertEquals("derp.nonexisting.com", loc.classIdentifier)
        assertEquals(true , loc.methodName.isPresent)
        assertEquals("IAmAMethodName", loc.methodName.get())
        assertEquals(true, loc.methodParameters.isPresent)
        assertEquals("(String, Integer, float)", loc.methodParameters.get())
    }
}
