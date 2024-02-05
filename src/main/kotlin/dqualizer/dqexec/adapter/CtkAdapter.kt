package dqualizer.dqexec.adapter



import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.config.StartupConfig
import io.github.dqualizer.dqlang.types.adapter.ctk.*
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.EnrichedArtifact
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.ResilienceTestConfiguration
import org.springframework.stereotype.Component

/**
 * Adapts a resilience test configuration to CTK tests
 * */
@Component
class CtkAdapter(private val startupConfig: StartupConfig)
{
    // $ pointers are used to reference the secretes defined in the top-level of the experiment definition
    val authenticationParameters = mapOf("db_username" to "\${db_username}", "db_password" to "\${db_password}", "username" to "\${username}", "password" to "\${password}")

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
            val secrets = createTopLevelSecrets()
            val steadyStateHypothesis = createSteadyStateHypothesisForUnaivalabilityStimulus(resilienceTestDefinition.artifact)
            val method = listOf(createActionToKillProcess(resilienceTestDefinition.artifact), createProbeToMonitorRecoveryTimeOfProcess(resilienceTestDefinition.artifact))
            val rollbacks = listOf(createActionToStartProcess(resilienceTestDefinition.artifact))
            val repetitions = 1 // TODO get value from accuracy defined in resilienceTestConfig

            val ctkChaosExperiment = CtkChaosExperiment(resilienceTestDefinition.description, resilienceTestDefinition.description, secrets, steadyStateHypothesis, method, rollbacks, repetitions);
            ctkChaosExperiments.add(ctkChaosExperiment)
        }
        return CtkConfiguration(resilienceTestConfig.context, ctkChaosExperiments)
    }


    /**
     * Creates a Secrets object which defines authentication secrets which point to the runtime environment of the CTK experiment
     */
    private fun createTopLevelSecrets(): Secrets {
        val dbUsername = startupConfig.getDbUsername()
        val dbPassword = startupConfig.getDbPassword()
        val username = startupConfig.getUsername()
        val password = startupConfig.getPassword()
        val authenticationSecret = AuthenticationSecret(username, password, dbUsername, dbPassword)
        return Secrets(authenticationSecret)
    }

    private fun createActionToStartProcess(artifact: EnrichedArtifact): Action {
        val actionName = "start process " + (artifact.processId)
        // TODO in the longterm these infos should be provided by DAM / filled into enrichedConfig
        val argumentsForFunction =  authenticationParameters + ("path" to artifact.processPath) + ("log_result_in_influx_db" to true)
        val actionProvider = Provider("python", "processStarting", "start_process_by_path", argumentsForFunction)

        return Action(actionName, actionProvider)
    }


    fun createSteadyStateHypothesisForUnaivalabilityStimulus(artifact: EnrichedArtifact): SteadyStateHypothesis {
        val name = "Application is running"
        return SteadyStateHypothesis(name, listOf(createProbeToLookIfProcessIsRunning(true, artifact)))
    }

    fun createProbeToLookIfProcessIsRunning(isSteadyStateHypothesis: Boolean, artifact: EnrichedArtifact): Probe {
        val probeName = artifact.processId + " must be running"
        // TODO in the longterm these infos should be provided by DAM / filled into enrichedConfig
        val argumentsForFunction =  authenticationParameters + ("process_name" to artifact.processId) + ("log_result_in_influx_db" to true)
        val probeProvider = Provider("python", "processMonitoring", "check_process_exists",argumentsForFunction)

        if (isSteadyStateHypothesis){
            val probeTolerance = ObjectMapper().convertValue<JsonNode>(true, JsonNode::class.java)
            return SteadyStateProbe(probeName, probeProvider, probeTolerance)
        }
        return Probe(probeName, probeProvider)
    }




    private fun createActionToKillProcess(artifact: EnrichedArtifact): Action {
        val actionName = "kill process " + artifact.processId
        // TODO in the longterm these infos should be provided by DAM / filled into enrichedConfig
        val argumentsForFunction =  authenticationParameters + ("process_name" to artifact.processId)
        val actionProvider = Provider("python", "processKilling", "kill_process_by_name", argumentsForFunction)

        return Action(actionName, actionProvider)
    }

    private fun createProbeToMonitorRecoveryTimeOfProcess(artifact: EnrichedArtifact): Probe {
        val probeName = "measure duration until process " + artifact.processId + " is eventually available again"
        // TODO in the longterm these infos should be provided by DAM / filled into enrichedConfig
        val argumentsForFunction =  authenticationParameters + ("process_name" to artifact.processId) +  ("monitoring_duration_sec" to 10) +  ("checking_interval_sec" to 0)
        val provider = Provider("python", "processMonitoring", "get_duration_until_process_started", argumentsForFunction)

        return Probe(probeName, provider)
    }

}