package dqualizer.dqexec.resilienceTestRunner

import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqexec.util.ProcessLogger
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.io.path.Path

class CtkRunnerTest {

    @Test
    fun testRunTestOnLocalChaosToolkit() {

        //arrange
        val processLogger = ProcessLogger()
        val ctkRunner = CtkRunner(processLogger, ResourcePaths())
        val experimentPath = Path("resources\\ctk\\generatedExperiments\\test_experiment.json")

        //act
        val result = ctkRunner.runTestOnLocalWindowsChaosToolkit(experimentPath, 1, 1 )

        //assert
        Assertions.assertEquals(0, result)
    }

}