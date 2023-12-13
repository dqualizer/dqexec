package dqualizer.dqexec.adapter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.dqualizer.dqlang.types.adapter.ctk.*
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.EnrichedArtifact
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.EnrichedResilienceTestDefinition
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.ResilienceTestConfiguration
import io.github.dqualizer.dqlang.types.rqa.definition.Artifact
import io.github.dqualizer.dqlang.types.rqa.definition.enums.Satisfaction
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.ResilienceResponseMeasures
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.stimulus.UnavailabilityStimulus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Map

class CtkAdapterTest {

    @Test
    fun testAdapt(){

        //arrange input resilienceTestConfiguration
        val ctkAdapter = CtkAdapter()

        val artifact = Artifact("aTestSystemId","aTestActivityId")
        val enrichedArtifact = EnrichedArtifact(artifact, "testProcessId.exe", "C:\\testPathToProcessToRestart")
        val stimulus = UnavailabilityStimulus("UNAVAILABILITY", 100)
        val responseMeasure = ResilienceResponseMeasures(Satisfaction.SATISFIED)
        val enrichedResilienceTestDefinition = EnrichedResilienceTestDefinition(enrichedArtifact, "testDescription", stimulus, responseMeasure)
        val resilienceTestConfiguration = ResilienceTestConfiguration("testVersion", "testContext", "testEnvironment", setOf(enrichedResilienceTestDefinition))

        // arrange expected CTK Experiment
        val objectMapper = ObjectMapper()
        val title = "testDescription"
        val description = "testDescription"

        val providerForSteadyStateProbe = Provider("python", "processMonitoring", "check_process_exists", Map.of<String, Any>("process_name", "testProcessId.exe"))
        val steadyStateProbe = SteadyStateProbe("testProcessId.exe must be running", providerForSteadyStateProbe, objectMapper.convertValue(true, JsonNode::class.java))
        val steadyStateHypothesis = SteadyStateHypothesis("Application is running", listOf<Probe>(steadyStateProbe))

        val providerForProbe = Provider("python", "processMonitoring", "get_duration_until_process_started",  Map.of<String, Any>("process_name", "testProcessId.exe", "monitoring_duration_sec", 10, "checking_interval_sec", 0))
        val probe = Probe("measure duration until process testProcessId.exe is eventually available again", providerForProbe)

        val providerForAction = Provider("python", "processKilling", "kill_process_by_name", Map.of<String, Any>("process_name", "testProcessId.exe"))
        val action = Action("kill process testProcessId.exe", providerForAction)
        val method = listOf(action, probe)

        val providerForRollbackAction = Provider("python", "processStarting", "start_process_by_path", Map.of<String, Any>("path", "C:\\testPathToProcessToRestart"))
        val actionForRollbacks = Action("start process testProcessId.exe", providerForRollbackAction)
        val rollbacks = listOf(actionForRollbacks)

        val ctkChaosExperiment = CtkChaosExperiment(title, description, steadyStateHypothesis, method, rollbacks)

        //act
        val result = ctkAdapter.adapt(resilienceTestConfiguration)

        //assert
        Assertions.assertEquals("testDescription" , result.ctkChaosExperiments.first().title)
        Assertions.assertEquals("testDescription" , result.ctkChaosExperiments.first().description)
        Assertions.assertEquals(steadyStateHypothesis , result.ctkChaosExperiments.first().steadyStateHypothesis)
        Assertions.assertEquals(method , result.ctkChaosExperiments.first().method)
        Assertions.assertEquals(rollbacks , result.ctkChaosExperiments.first().rollbacks)
        Assertions.assertEquals(ctkChaosExperiment, result.ctkChaosExperiments.first())
    }
}