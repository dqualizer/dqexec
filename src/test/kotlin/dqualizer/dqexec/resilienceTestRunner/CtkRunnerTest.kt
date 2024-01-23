package dqualizer.dqexec.resilienceTestRunner

import dqualizer.dqexec.config.ResourcePaths
import dqualizer.dqexec.util.ProcessLogger
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class CtkRunnerTest {

    @Test
    fun testRunTestOnLocalChaosToolkit() {

        //arrange
        val processLogger = ProcessLogger()
        val ctkRunner = CtkRunner(processLogger, ResourcePaths())
        val experimentPath = Path("resources\\ctk\\generatedExperiments\\TestDescription_experiment.json")

        //act
        val result = ctkRunner.runExperimentOnLocalWindowsChaosToolkit(experimentPath, 1, 1 )

        //assert
        Assertions.assertEquals(0, result)
    }

    @Test
    fun testRequestExperimentExecutionOnHost() {

        //arrange
        val processLogger = ProcessLogger()
        val ctkRunner = CtkRunner(processLogger, ResourcePaths())
        val experimentFilename = "TestDescription_experiment.json"

        //act
        val result = ctkRunner.requestExperimentExecutionOnHost(experimentFilename, 1, 1 )

        //assert
        Assertions.assertEquals(0, result)
    }

}