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

            // TODO consider accuracy defined in resilienceTestConfig for multiple repetitions
            val repetitions = 1
            lateinit var ctkChaosExperiment: CtkChaosExperiment

            if (resilienceTestDefinition.stimulus is UnavailabilityStimulus){
                val secrets = createTopLevelSecrets()
                val steadyStateHypothesis = createSteadyStateHypothesisForUnaivalabilityStimulus(resilienceTestDefinition.artifact)
                val method = listOf(createActionToKillProcess(resilienceTestDefinition.artifact),
                        createProbeToMonitorRecoveryTimeOfProcess(resilienceTestDefinition.artifact))
                val rollbacks = listOf(createActionToStartProcess(resilienceTestDefinition.artifact))
                ctkChaosExperiment = CtkChaosExperiment(resilienceTestDefinition.description, resilienceTestDefinition.description, method, repetitions)
                ctkChaosExperiment.secrets = secrets
                ctkChaosExperiment.steadyStateHypothesis = steadyStateHypothesis
                ctkChaosExperiment.rollbacks = rollbacks
            }

            else if (resilienceTestDefinition.stimulus is LateResponsesStimulus){
                // secrets and Steady State Hypothesis are not necessary for this kind of experiments yet
                val method = listOf(createActionToEnableChaosMonkeyForSpringBoot(resilienceTestDefinition.artifact),
                        createActionToConfigureAssaults(resilienceTestDefinition.artifact),
                        createActionToChangeWatcherConfiguration(resilienceTestDefinition.artifact))
                val rollbacks = listOf(createActionToDisableChaosMonkeyForSpringBoot(resilienceTestDefinition.artifact))
                ctkChaosExperiment = CtkChaosExperiment(resilienceTestDefinition.description, resilienceTestDefinition.description, method, repetitions)
                ctkChaosExperiment.rollbacks = rollbacks
            }
            ctkChaosExperiments.add(ctkChaosExperiment)
        }
        return CtkConfiguration(resilienceTestConfig.context, ctkChaosExperiments)
    }

    /**
     * Creates a Secrets object which defines authentication secrets which are used by experiment probes and actions
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
        val argumentsForFunction = mapOf("base_url" to "${artifact.baseUrl}")
        val provider = Provider("python", "chaosspring.actions", "enable_chaosmonkey", argumentsForFunction)

        return Action(actionName, provider)
    }

    private fun createActionToConfigureAssaults(artifact: EnrichedArtifact): Action{
        val actionName = "configure_assaults"
        // TODO move this to DAM, should be translated to stimulus, which is then additional input for this method
        val assaultsConfiguration = object {
            val level: Int = 1
            val deterministic: String = "True"
            val latencyRangeStart: Int = 2000
            val latencyRangeEnd: Int = 2000
            val latencyActive: String = "True"
            val exceptionsActive: String = "false"
            val killApplicationActive: String = "false"
            val restartApplicationActive: String = "false"
            // this can also inhabit beans other than services, e.g. a repo-class or method see https://codecentric.github.io/chaos-monkey-spring-boot/latest/
            val watchedCustomServices:List<String> = listOf(artifact.packageMember)
        }

        val argumentsForFunction = mapOf("base_url" to "${artifact.baseUrl}", "assaults_configuration" to assaultsConfiguration)
        val provider = Provider("python", "chaosspring.actions", "change_assaults_configuration", argumentsForFunction)

        return Action(actionName, provider)
    }

    // Also if the assaultConfiguration already adds a watched service, also the corresponding watcher needs to be enabled for the failure injection to work for the watchedCustomService
    private fun createActionToChangeWatcherConfiguration(artifact: EnrichedArtifact):Action{
        val actionName = "configure_watchers"
        val watchersConfiguration = object {
            var controller: String = "false"
            var restController: String = "false"
            var service: String = "false"
            var repository: String = "false"
            var component: String = "false"
            val restTemplate: String = "false"
            val webClient: String = "false"
            val actuatorHealth: String = "false"
            // TODO maybe it also works when member name entered in the DAM is just put into "beans" List?
            val beans: List<String> = emptyList()
            val beanClasses: List<String> = emptyList()
            val excludeClasses: List<String> = emptyList()
        }

        // check on the member name entered in the DAM which type of Beans should be watched in the experiment
        // this assumes that e.g. Spring Repository Beans contain "Repo" in their class name to work
        when {
            "controller" in artifact.packageMember.lowercase() -> watchersConfiguration.controller = "true"
            "restController" in artifact.packageMember.lowercase() -> watchersConfiguration.restController = "true"
            "repo" in artifact.packageMember.lowercase() -> watchersConfiguration.repository = "true"
            "controller" in artifact.packageMember.lowercase() -> watchersConfiguration.controller = "true"
            "component" in artifact.packageMember.lowercase() -> watchersConfiguration.component = "true"
        }

        val argumentsForFunction = mapOf("base_url" to "${artifact.baseUrl}", "watchers_configuration" to watchersConfiguration)
        // TODO Needs be written in chaostoolkit extension and then added as function
        val provider = Provider("python", "chaosspring.actions", "change_watchers_configuration", argumentsForFunction)
        return Action(actionName, provider)
    }

    private fun createActionToDisableChaosMonkeyForSpringBoot(artifact: EnrichedArtifact): Action {
        val actionName = "disable_chaosmonkey"
        val argumentsForFunction = mapOf("base_url" to "${artifact.baseUrl}")
        val provider = Provider("python", "chaosspring.actions", "disable_chaosmonkey", argumentsForFunction)

        return Action(actionName, provider)
    }


}