package dqualizer.dqexec.adapter

import io.github.dqualizer.dqlang.types.adapter.ctk.Action
import io.github.dqualizer.dqlang.types.adapter.ctk.CtkChaosExperiment
import io.github.dqualizer.dqlang.types.adapter.ctk.Pauses
import io.github.dqualizer.dqlang.types.adapter.ctk.Provider
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.EnrichedCmsbArtifact
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.EnrichedResilienceTestDefinition
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.stimulus.FailedRequestsStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.stimulus.LateResponsesStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.stimulus.ResilienceStimulus
import org.springframework.stereotype.Component

@Component
class CmsbFailedDelayedStimulusAdapter {
    fun createExperimentForLateResponsesStimulus(enrichedResilienceTestDefinition: EnrichedResilienceTestDefinition): CtkChaosExperiment {
        val method = listOf(createActionToConfigureAssaultsForLateResponseStimulus(enrichedResilienceTestDefinition.enrichedCmsbArtifact, enrichedResilienceTestDefinition.stimulus as LateResponsesStimulus),
                createActionToChangeWatcherConfiguration(enrichedResilienceTestDefinition.enrichedCmsbArtifact),
                createActionToEnableChaosMonkeyForSpringBoot(enrichedResilienceTestDefinition.enrichedCmsbArtifact, enrichedResilienceTestDefinition.stimulus))
        val rollbacks = listOf(createActionToDisableChaosMonkeyForSpringBoot(enrichedResilienceTestDefinition.enrichedCmsbArtifact))

        // secrets and Steady State Hypothesis are not necessary for this kind of experiments yet
        return CtkChaosExperiment(enrichedResilienceTestDefinition.name, enrichedResilienceTestDefinition.description, null, null, method, rollbacks, null)
    }

    fun createExperimentForFailedRequestsStimulus(enrichedResilienceTestDefinition: EnrichedResilienceTestDefinition): CtkChaosExperiment {
        val method = listOf(
                createActionToConfigureAssaultsForFailedRequestStimulus(enrichedResilienceTestDefinition.enrichedCmsbArtifact, enrichedResilienceTestDefinition.stimulus as FailedRequestsStimulus),
                createActionToChangeWatcherConfiguration(enrichedResilienceTestDefinition.enrichedCmsbArtifact),
                createActionToEnableChaosMonkeyForSpringBoot(enrichedResilienceTestDefinition.enrichedCmsbArtifact, enrichedResilienceTestDefinition.stimulus),
        )
        val rollbacks = listOf(createActionToDisableChaosMonkeyForSpringBoot(enrichedResilienceTestDefinition.enrichedCmsbArtifact))

        // secrets and Steady State Hypothesis are not necessary for this kind of experiments yet
        return CtkChaosExperiment(enrichedResilienceTestDefinition.name, enrichedResilienceTestDefinition.description, null, null, method, rollbacks, null)
    }

    private fun createActionToEnableChaosMonkeyForSpringBoot(artifact: EnrichedCmsbArtifact, stimulus: ResilienceStimulus): Action {
        val actionName = "enable_chaosmonkey"
        val argumentsForFunction = mapOf("base_url" to artifact.baseUrl)
        val provider = Provider("python", "chaosspring.actions", "enable_chaosmonkey", argumentsForFunction)
        val pauses = Pauses(stimulus.pauseBeforeTriggeringSeconds, stimulus.experimentDurationSeconds)

        return Action(actionName, provider, pauses)
    }

    private fun createActionToConfigureAssaultsForLateResponseStimulus(artifact: EnrichedCmsbArtifact, stimulus: LateResponsesStimulus): Action {
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

        val argumentsForFunction = mapOf("base_url" to artifact.baseUrl, "assaults_configuration" to assaultsConfiguration)
        val provider = Provider("python", "chaosspring.actions", "change_assaults_configuration", argumentsForFunction)

        return Action(actionName, provider)
    }

    private fun createActionToConfigureAssaultsForFailedRequestStimulus(artifact: EnrichedCmsbArtifact, stimulus: FailedRequestsStimulus): Action {
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

        val argumentsForFunction = mapOf("base_url" to artifact.baseUrl, "assaults_configuration" to assaultsConfiguration)
        val provider = Provider("python", "chaosspring.actions", "change_assaults_configuration", argumentsForFunction)

        return Action(actionName, provider)
    }

    // Also if the assaultConfiguration already adds a watched service, also the corresponding watcher needs to be enabled for the failure injection to work for the watchedCustomService
    private fun createActionToChangeWatcherConfiguration(artifact: EnrichedCmsbArtifact): Action {
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

        val argumentsForFunction = mapOf("base_url" to artifact.baseUrl, "watchers_configuration" to watchersConfiguration)
        val provider = Provider("python", "chaosspring.actions", "change_watchers_configuration", argumentsForFunction)
        return Action(actionName, provider)
    }

    private fun createActionToDisableChaosMonkeyForSpringBoot(artifact: EnrichedCmsbArtifact): Action {
        val actionName = "disable_chaosmonkey"
        val argumentsForFunction = mapOf("base_url" to artifact.baseUrl)
        val provider = Provider("python", "chaosspring.actions", "disable_chaosmonkey", argumentsForFunction)

        return Action(actionName, provider)
    }
}