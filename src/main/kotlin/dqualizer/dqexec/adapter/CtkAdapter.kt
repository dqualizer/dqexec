package dqualizer.dqexec.adapter



import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.dqualizer.dqlang.types.adapter.ctk.*
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.EnrichedArtifact
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.EnrichedResilienceTestDefinition
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.ResilienceTestConfiguration
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.stimulus.UnavailabilityStimulus
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component

/**
 * Adapts a resilience test configuration to CTK tests
 * */
@Component
class CtkAdapter()
{

    /**
     * Adapt the loadtest configuration. It consists of 3 steps:
     * 1. Adapt global test configuration
     * 2. Adapt the stimulus for every loadtest
     * 3. Adapt the endpoints for every loadtest
     * @param resilienceTestConfig The received dqlang load test configuration
     * @return An adapted loadtest configuration for k6
     */
    fun adapt(resilienceTestConfig: ResilienceTestConfiguration): CtkConfiguration {
        val ctkChaosExperiments = LinkedHashSet<CtkChaosExperiment>()
        for (resilienceTestDefinition in resilienceTestConfig.enrichedResilienceTestDefinitions) {
            val steadyStateHypothesis = createSteadyStateHypothesisForUnaivalabilityStimulus(resilienceTestDefinition.artifact)
            val method = listOf(createActionToKillProcess(resilienceTestDefinition.artifact), createProbeToMonitorRecoveryTimeOfProcess(resilienceTestDefinition.artifact))
            val rollbacks = listOf(createActionToStartProcess(resilienceTestDefinition.artifact))
            val repitions = 1 // TODO get value from accuracy defined in resilienceTestConfig

            val ctkChaosExperiment = CtkChaosExperiment(resilienceTestDefinition.description, resilienceTestDefinition.description, steadyStateHypothesis, method, rollbacks, repitions);
            ctkChaosExperiments.add(ctkChaosExperiment)
        }
        return CtkConfiguration(resilienceTestConfig.context, ctkChaosExperiments)
    }

    private fun createActionToStartProcess(artifact: EnrichedArtifact): Action {
        val actionName = "start process " + (artifact.processId)
        // TODO in the longterm these infos should be provided by DAM / filled into enrichedConfig
        val actionProvider = Provider("python", "processStarting", "start_process_by_path", mapOf("path" to (artifact.processPath
                ?: null)))

        return Action(actionName, actionProvider)
    }


    fun createSteadyStateHypothesisForUnaivalabilityStimulus(artifact: EnrichedArtifact): SteadyStateHypothesis {
        val name = "Application is running"
        return SteadyStateHypothesis(name, listOf(createProbeToLookIfProcessIsRunning(true, artifact)))
    }

    fun createProbeToLookIfProcessIsRunning(isSteadyStateHypothesis: Boolean, artifact: EnrichedArtifact): Probe {
        val probeName = artifact.processId + " must be running"
        // TODO in the longterm these infos should be provided by DAM / filled into enrichedConfig
        val probeProvider = Provider("python", "processMonitoring", "check_process_exists", mapOf("process_name" to artifact.processId, "log_result_in_influx_db" to true))

        if (isSteadyStateHypothesis){
            val probeTolerance = ObjectMapper().convertValue<JsonNode>(true, JsonNode::class.java)
            return SteadyStateProbe(probeName, probeProvider, probeTolerance)
        }
        return Probe(probeName, probeProvider)
    }


    private fun createActionToKillProcess(artifact: EnrichedArtifact): Action {
        val actionName = "kill process " + artifact.processId
        // TODO in the longterm these infos should be provided by DAM / filled into enrichedConfig
        val actionProvider = Provider("python", "processKilling", "kill_process_by_name", mapOf("process_name" to artifact.processId))

        return Action(actionName, actionProvider)
    }

    private fun createProbeToMonitorRecoveryTimeOfProcess(artifact: EnrichedArtifact): Probe {
        val probeName = "measure duration until process " + artifact.processId + " is eventually available again"
        // TODO in the longterm these infos should be provided by DAM / filled into enrichedConfig
        val provider = Provider("python", "processMonitoring", "get_duration_until_process_started", mapOf("process_name" to artifact.processId, "monitoring_duration_sec" to 10, "checking_interval_sec" to 0))

        return Probe(probeName, provider)
    }

}