package dqualizer.dqexec.adapter.resilience

import io.github.dqualizer.dqlang.types.adapter.ctk.*
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.ResilienceTestConfiguration
import io.github.dqualizer.dqlang.types.rqa.definition.resilience.stimulus.UnavailabilityStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.resilience.stimulus.FailedRequestsStimulus
import io.github.dqualizer.dqlang.types.rqa.definition.resilience.stimulus.LateResponsesStimulus
import org.springframework.stereotype.Component

/**
 * Adapts a resilience test configuration to CTK tests
 */
@Component
class CtkAdapter(
  private val processUnavailabilityAdapter: ProcessUnavailabilityAdapter,
  private val cmsbFailedDelayedStimulusAdapter: CmsbFailedDelayedStimulusAdapter
) {

    fun adapt(resilienceTestConfig: ResilienceTestConfiguration): CtkConfiguration {
        val ctkChaosExperiments = LinkedHashSet<CtkChaosExperiment>()
        for (enrichedResilienceTestDefinition in resilienceTestConfig.resilienceTestArtifacts) {
            lateinit var ctkChaosExperiment: CtkChaosExperiment

            if (enrichedResilienceTestDefinition.stimulus is UnavailabilityStimulus){
                ctkChaosExperiment = processUnavailabilityAdapter.createExperimentForUnavailabilityStimulus(enrichedResilienceTestDefinition)
            }

            else if (enrichedResilienceTestDefinition.stimulus is LateResponsesStimulus){
                ctkChaosExperiment = cmsbFailedDelayedStimulusAdapter.createExperimentForLateResponsesStimulus(enrichedResilienceTestDefinition)
            }

            else if (enrichedResilienceTestDefinition.stimulus is FailedRequestsStimulus){
                ctkChaosExperiment = cmsbFailedDelayedStimulusAdapter.createExperimentForFailedRequestsStimulus(enrichedResilienceTestDefinition)
            }
            ctkChaosExperiments.add(ctkChaosExperiment)
        }
        return CtkConfiguration(resilienceTestConfig.context, ctkChaosExperiments)
    }
}