package dqualizer.dqexec.adapter



import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.config.StartupConfig
import io.github.dqualizer.dqlang.types.adapter.ctk.*
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.EnrichedArtifact
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.ResilienceTestConfiguration
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.stimulus.LateResponsesStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.stimulus.UnavailabilityStimulus
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

            lateinit var secrets: Secrets
            lateinit var steadyStateHypothesis: SteadyStateHypothesis
            lateinit var method: List<Probe>
            lateinit var rollbacks: List<Action>
            // TODO consider accuracy defined in resilienceTestConfig for multiple repetitions
            val repetitions = 1

            if (resilienceTestDefinition.stimulus is UnavailabilityStimulus){
                secrets = createTopLevelSecrets()
                steadyStateHypothesis = createSteadyStateHypothesisForUnaivalabilityStimulus(resilienceTestDefinition.artifact)
                method = listOf(createActionToKillProcess(resilienceTestDefinition.artifact),
                        createProbeToMonitorRecoveryTimeOfProcess(resilienceTestDefinition.artifact))
                rollbacks = listOf(createActionToStartProcess(resilienceTestDefinition.artifact))
            }

            else if (resilienceTestDefinition.stimulus is LateResponsesStimulus){
                // secrets and SS Hypothesis are not necessary for this kind of experiments yet
                secrets = Secrets()
                steadyStateHypothesis = SteadyStateHypothesis()
                method = listOf(createActionToEnableChaosMonkeyForSpringBoot(resilienceTestDefinition.artifact),
                        createActionToConfigureAssaults(resilienceTestDefinition.artifact),
                        createActionToChangeWatcherConfiguration(resilienceTestDefinition.artifact))
                rollbacks = listOf(createActionToDisableChaosMonkeyForSpringBoot(resilienceTestDefinition.artifact))
            }

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


    private fun createActionToEnableChaosMonkeyForSpringBoot(artifact: EnrichedArtifact): Action{
        val actionName = "enable_chaosmonkey"
        val argumentsForFunction = mapOf("base_url" to "${artifact.baseUrl}/actuator")
        val provider = Provider("python", "chaosspring.actions", "enable_chaosmonkey", argumentsForFunction)

        return Action(actionName, provider)
    }

    private fun createActionToConfigureAssaults(artifact: EnrichedArtifact): Action{
        val actionName = "configure_assaults"
        // TODO move this to DAM, should be translated to stimulus, which is then additional input for this method
        val assaultsConfiguration = object {
            val level: Int = 1
            val deterministic: Boolean = true
            val latencyRangeStart: Int = 2000
            val latencyRangeEnd: Int = 2000
            val latencyActive: Boolean = true
            val exceptionsActive: Boolean = false
            val killApplicationActive: Boolean = false
            val restartApplicationActive: Boolean = false
            // this can also inhabit beans other than services, e.g. a repo-class or method see https://codecentric.github.io/chaos-monkey-spring-boot/latest/
            val watchedCustomServices:List<String> = listOf(artifact.processId)
        }

        val argumentsForFunction = mapOf("base_url" to "${artifact.baseUrl}/actuator", "assault_configuration" to assaultsConfiguration)
        val provider = Provider("python", "chaosspring.actions", "change_assaults_configuration", argumentsForFunction)

        return Action(actionName, provider)
    }

    // Also if the assaultConfiguration already adds a watched service, also the corresponding watcher needs to be enabled for the failure injection to work for the watchedCustomService
    private fun createActionToChangeWatcherConfiguration(artifact: EnrichedArtifact):Action{
        val actionName = "configure_watchers"
        val watcherConfiguration = object {
            var controller: Boolean = false
            var restController: Boolean = false
            var service: Boolean = false
            var repository: Boolean = false
            var component: Boolean = false
            val restTemplate: Boolean = false
            val webClient: Boolean = false
            val actuatorHealth: Boolean = false
            // TODO maybe it also works when member name entered in the DAM is just put into "beans" List?
            val beans: List<String> = emptyList()
            val beanClasses: List<String> = emptyList()
            val excludeClasses: List<String> = emptyList()
        }

        // check on the member name entered in the DAM which type of Beans should be watched in the experiment; this assumes that e.g. Spring Repository Beans contain "Repo" in their class name
        when {
            "controller" in artifact.packageMember.lowercase() -> watcherConfiguration.controller = true
            "restController" in artifact.packageMember.lowercase() -> watcherConfiguration.restController = true
            "repo" in artifact.packageMember.lowercase() -> watcherConfiguration.repository = true
            "controller" in artifact.packageMember.lowercase() -> watcherConfiguration.controller = true
            "component" in artifact.packageMember.lowercase() -> watcherConfiguration.component = true
        }

        val argumentsForFunction = mapOf("base_url" to "${artifact.baseUrl}/actuator", "watcher_configuration" to watcherConfiguration)
        // TODO Needs be written in chaostoolkit extension and then added as function
        val provider = Provider("python", "chaosspring.actions", "", argumentsForFunction)
        return Action(actionName, provider)
    }

    private fun createActionToDisableChaosMonkeyForSpringBoot(artifact: EnrichedArtifact): Action {
        val actionName = "disable_chaosmonkey"
        val argumentsForFunction = mapOf("base_url" to "${artifact.baseUrl}/actuator")
        val provider = Provider("python", "chaosspring.actions", "disable_chaosmonkey", argumentsForFunction)

        return Action(actionName, provider)
    }


}