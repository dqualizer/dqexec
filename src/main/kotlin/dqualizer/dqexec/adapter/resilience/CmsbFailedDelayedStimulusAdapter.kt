package dqualizer.dqexec.adapter.resilience

import io.github.dqualizer.dqlang.types.adapter.ctk.*
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.CmsbArtifact
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.ResilienceTestArtifact
import io.github.dqualizer.dqlang.types.rqa.definition.resilience.stimulus.FailedRequestsStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.resilience.stimulus.LateResponsesStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.resilience.stimulus.ResilienceStimulus
import org.springframework.stereotype.Component

@Component
class CmsbFailedDelayedStimulusAdapter {
    fun createExperimentForLateResponsesStimulus(resilienceTestArtifact: ResilienceTestArtifact): CtkChaosExperiment {
      val csmbArtifact = resilienceTestArtifact.cmsbArtifact!!
      val method = listOf(
        createActionToConfigureAssaultsForLateResponseStimulus(csmbArtifact, resilienceTestArtifact.stimulus as LateResponsesStimulus),
        //createActionToChangeWatcherConfiguration(csmbArtifact),
        createActionToEnableChaosMonkeyForSpringBoot(csmbArtifact, resilienceTestArtifact.stimulus!!)
      )
      val rollbacks = listOf(createActionToDisableChaosMonkeyForSpringBoot(csmbArtifact))

      // secrets and Steady State Hypothesis are not necessary for this kind of experiments yet,
      // but we need to provide something non-None in Python
      val hypothesis = SteadyStateHypothesis(title = "default", probes = emptyList())
      val secrets = Secrets(AuthenticationSecret())

      return CtkChaosExperiment(resilienceTestArtifact.name, resilienceTestArtifact.description, secrets, hypothesis, method, rollbacks, emptyList(), null)
    }

    fun createExperimentForFailedRequestsStimulus(resilienceTestArtifact: ResilienceTestArtifact): CtkChaosExperiment {
      val csmbArtifact = resilienceTestArtifact.cmsbArtifact!!
      val method = listOf(
                createActionToConfigureAssaultsForFailedRequestStimulus(csmbArtifact, resilienceTestArtifact.stimulus as FailedRequestsStimulus),
                //createActionToChangeWatcherConfiguration(csmbArtifact),
                createActionToEnableChaosMonkeyForSpringBoot(csmbArtifact, resilienceTestArtifact.stimulus!!),
        )
        val rollbacks = listOf(createActionToDisableChaosMonkeyForSpringBoot(csmbArtifact))

        // secrets and Steady State Hypothesis are not necessary for this kind of experiments yet,
        // but we need to provide something non-None in Python
        val hypothesis = SteadyStateHypothesis(title = "default", probes = emptyList())
        val secrets = Secrets(AuthenticationSecret())

        return CtkChaosExperiment(resilienceTestArtifact.name, resilienceTestArtifact.description, secrets, hypothesis, method, rollbacks, emptyList(), null)
    }

    private fun createActionToEnableChaosMonkeyForSpringBoot(artifact: CmsbArtifact, stimulus: ResilienceStimulus): Action {
        val actionName = "enable_chaosmonkey"
        val argumentsForFunction = mapOf("base_url" to artifact.baseUrl)
        val provider = Provider("python", "chaosspring.actions", "enable_chaosmonkey", argumentsForFunction)
        val pauses = Pauses(stimulus.pauseBeforeTriggeringSeconds, stimulus.experimentDurationSeconds)

        return Action(name = actionName, pauses = pauses, provider = provider)
    }

    private fun createActionToConfigureAssaultsForLateResponseStimulus(artifact: CmsbArtifact, stimulus: LateResponsesStimulus): Action {
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
        val pauses = Pauses(stimulus.pauseBeforeTriggeringSeconds, stimulus.experimentDurationSeconds)

        return Action(name = actionName, provider = provider, pauses = pauses)
    }

    private fun createActionToConfigureAssaultsForFailedRequestStimulus(artifact: CmsbArtifact, stimulus: FailedRequestsStimulus): Action {
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
        val pauses = Pauses(stimulus.pauseBeforeTriggeringSeconds, stimulus.experimentDurationSeconds)

        return Action(name = actionName, provider = provider, pauses = pauses)
    }

    /*
      TODO  Do we need this method?
            Apparently, there is no function "change_watchers_configuration" provided by CTK
     */
    // Also if the assaultConfiguration already adds a watched service, also the corresponding watcher needs to be enabled for the failure injection to work for the watchedCustomService
    private fun createActionToChangeWatcherConfiguration(artifact: CmsbArtifact): Action {
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
        // Hardcoded pause, since it can't be null
        val pauses = Pauses(2, 5)
        return Action(name = actionName, provider = provider, pauses = pauses)
    }

    private fun createActionToDisableChaosMonkeyForSpringBoot(artifact: CmsbArtifact): Action {
        val actionName = "disable_chaosmonkey"
        val argumentsForFunction = mapOf("base_url" to artifact.baseUrl)
        val provider = Provider("python", "chaosspring.actions", "disable_chaosmonkey", argumentsForFunction)
        // Hardcoded pause, since it can't be null
        val pauses = Pauses(2, 5)

        return Action(name = actionName, provider = provider, pauses = pauses)
    }
}