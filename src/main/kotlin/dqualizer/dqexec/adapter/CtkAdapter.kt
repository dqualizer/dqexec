package dqualizer.dqexec.adapter



import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.exception.UnknownTermException
import io.github.dqualizer.dqlang.types.adapter.constants.resilienceTesting.ResilienceTestConstants
import dqualizer.dqexec.config.StartupConfig
import io.github.dqualizer.dqlang.types.adapter.ctk.*
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.EnrichedArtifact
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.ResilienceTestConfiguration
import io.github.dqualizer.dqlang.types.rqa.definition.enums.Satisfaction
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.ResilienceResponseMeasures
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.stimulus.FailedRequestsStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.stimulus.LateResponsesStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.stimulus.ResilienceStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.stimulus.UnavailabilityStimulus
import org.springframework.stereotype.Component

/**
 * Adapts a resilience test configuration to CTK tests
 * */
// TODO split up logic into multiple classes
@Component
class CtkAdapter(private val startupConfig: StartupConfig, private val processUnavailabilityAdapter: ProcessUnavailabilityAdapter)
{


    fun adapt(resilienceTestConfig: ResilienceTestConfiguration): CtkConfiguration {
        val ctkChaosExperiments = LinkedHashSet<CtkChaosExperiment>()
        for (enrichedResilienceTestDefinition in resilienceTestConfig.enrichedResilienceTestDefinitions) {
            lateinit var ctkChaosExperiment: CtkChaosExperiment

            if (enrichedResilienceTestDefinition.stimulus is UnavailabilityStimulus){
                ctkChaosExperiment = processUnavailabilityAdapter.createExperimentForUnavailabilityStimulus(enrichedResilienceTestDefinition)

            }

            else if (enrichedResilienceTestDefinition.stimulus is LateResponsesStimulus){
                // secrets and Steady State Hypothesis are not necessary for this kind of experiments yet
                val method = listOf(createActionToConfigureAssaultsForLateResponseStimulus(enrichedResilienceTestDefinition.artifact, enrichedResilienceTestDefinition.stimulus as LateResponsesStimulus),
                        createActionToChangeWatcherConfiguration(enrichedResilienceTestDefinition.artifact, enrichedResilienceTestDefinition.stimulus),
                        createActionToEnableChaosMonkeyForSpringBoot(enrichedResilienceTestDefinition.artifact, enrichedResilienceTestDefinition.stimulus))
                val rollbacks = listOf(createActionToDisableChaosMonkeyForSpringBoot(enrichedResilienceTestDefinition.artifact))
                ctkChaosExperiment = CtkChaosExperiment(enrichedResilienceTestDefinition.name, enrichedResilienceTestDefinition.description, method)
                ctkChaosExperiment.rollbacks = rollbacks
            }

            else if (enrichedResilienceTestDefinition.stimulus is FailedRequestsStimulus){
                // secrets and Steady State Hypothesis are not necessary for this kind of experiments yet
                val method = listOf(createActionToConfigureAssaultsForFailedRequestStimulus(enrichedResilienceTestDefinition.artifact, enrichedResilienceTestDefinition.stimulus as FailedRequestsStimulus),
                        createActionToChangeWatcherConfiguration(enrichedResilienceTestDefinition.artifact, enrichedResilienceTestDefinition.stimulus),
                        createActionToEnableChaosMonkeyForSpringBoot(enrichedResilienceTestDefinition.artifact, enrichedResilienceTestDefinition.stimulus),
                       )
                val rollbacks = listOf(createActionToDisableChaosMonkeyForSpringBoot(enrichedResilienceTestDefinition.artifact))
                ctkChaosExperiment = CtkChaosExperiment(enrichedResilienceTestDefinition.name, enrichedResilienceTestDefinition.description, method)
                ctkChaosExperiment.rollbacks = rollbacks
            }


            ctkChaosExperiments.add(ctkChaosExperiment)
        }
        return CtkConfiguration(resilienceTestConfig.context, ctkChaosExperiments)
    }






    private fun createActionToEnableChaosMonkeyForSpringBoot(artifact: EnrichedArtifact, stimulus: ResilienceStimulus): Action{
        val actionName = "enable_chaosmonkey"
        val argumentsForFunction = mapOf("base_url" to "${artifact.baseUrl}")
        val provider = Provider("python", "chaosspring.actions", "enable_chaosmonkey", argumentsForFunction)
        val pauses = Pauses(stimulus.pauseBeforeTriggeringSeconds, stimulus.experimentDurationSeconds)

        return Action(actionName, provider, pauses)
    }

    private fun createActionToConfigureAssaultsForLateResponseStimulus(artifact: EnrichedArtifact, stimulus: LateResponsesStimulus): Action{
        val actionName = "configure_assaults"
        val assaultsConfiguration = object {
            val level: Int = stimulus.injectionFrequency
            val deterministic: String = "True"
            val latencyRangeStart: Int = stimulus.minDelayMilliseconds
            val latencyRangeEnd: Int = stimulus.maxDelayMilliseconds
            val latencyActive: String = "True"
            val exceptionsActive: String = "False"
            val killApplicationActive: String = "False"
            val restartApplicationActive: String = "False"
            // this can also inhabit beans other than services, e.g. a repo-class or method see https://codecentric.github.io/chaos-monkey-spring-boot/latest/
            val watchedCustomServices:List<String> = listOf(artifact.packageMember)
            }

        val argumentsForFunction = mapOf("base_url" to "${artifact!!.baseUrl}", "assaults_configuration" to assaultsConfiguration)
        val provider = Provider("python", "chaosspring.actions", "change_assaults_configuration", argumentsForFunction)

        return Action(actionName, provider)
    }

    private fun createActionToConfigureAssaultsForFailedRequestStimulus(artifact: EnrichedArtifact, stimulus: FailedRequestsStimulus): Action{
        val actionName = "configure_assaults"

        val assaultsConfiguration = object {
            val level: Int = stimulus.injectionFrequency
            val deterministic: String = "True"
            val latencyActive: String = "False"
            val exceptionsActive: String = "True"
            // Runtime Exception which Chaos Monkey for Spring Boot defines per default for explicit knowledge, would be set by CMSB even if not defined here
            val exception = mapOf(
                    "type" to "java.lang.RuntimeException",
                    "method" to "<init>",
                    "arguments" to listOf(
                            mapOf(
                                    "type" to "java.lang.String",
                                    "value" to "Chaos Monkey - RuntimeException"
                            )
                    )
            )
            val killApplicationActive: String = "False"
            val restartApplicationActive: String = "False"
            // this can also inhabit beans other than services, e.g. a repo-class or method see https://codecentric.github.io/chaos-monkey-spring-boot/latest/
            val watchedCustomServices:List<String> = listOf(artifact.packageMember)
        }

        val argumentsForFunction = mapOf("base_url" to "${artifact!!.baseUrl}", "assaults_configuration" to assaultsConfiguration)
        val provider = Provider("python", "chaosspring.actions", "change_assaults_configuration", argumentsForFunction)

        return Action(actionName, provider)
    }

    // Also if the assaultConfiguration already adds a watched service, also the corresponding watcher needs to be enabled for the failure injection to work for the watchedCustomService
    private fun createActionToChangeWatcherConfiguration(artifact: EnrichedArtifact, stimulus: ResilienceStimulus):Action{
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
        // as long as we have a customWatchedService set (bad naming, can also be method or other bean), all watchers could be activated without an effect
        // but do not do this to be conservative as possible with chaos injection
        when {
            "controller" in artifact.packageMember.lowercase() -> {watchersConfiguration.controller = "true"
                                                                    watchersConfiguration.restController = "true"}
            "service" in artifact.packageMember.lowercase() -> watchersConfiguration.service = "true"
            "restController" in artifact.packageMember.lowercase() -> watchersConfiguration.restController = "true"
            "repo" in artifact.packageMember.lowercase() -> watchersConfiguration.repository = "true"
            "controller" in artifact.packageMember.lowercase() -> watchersConfiguration.controller = "true"
            "component" in artifact.packageMember.lowercase() -> watchersConfiguration.component = "true"
        }

        val argumentsForFunction = mapOf("base_url" to "${artifact.baseUrl}", "watchers_configuration" to watchersConfiguration)
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