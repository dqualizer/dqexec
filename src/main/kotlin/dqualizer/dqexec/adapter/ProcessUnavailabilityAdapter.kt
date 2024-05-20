package dqualizer.dqexec.adapter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.exception.UnknownTermException
import io.github.dqualizer.dqlang.types.adapter.constants.resilienceTesting.ResilienceTestConstants
import io.github.dqualizer.dqlang.types.adapter.ctk.*
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.EnrichedProcessArtifact
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.EnrichedResilienceTestDefinition
import io.github.dqualizer.dqlang.types.rqa.definition.enums.Satisfaction
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.ResilienceResponseMeasures
import io.github.dqualizer.dqlang.types.rqa.definition.resiliencetest.stimulus.ResilienceStimulus
import org.springframework.stereotype.Component

@Component
class ProcessUnavailabilityAdapter(private val resilienceTestConstants: ResilienceTestConstants) {

    // $ pointers are used to reference the secretes defined in the top-level of the experiment definition
    val authenticationParameters = mapOf("db_username" to "\${db_username}", "db_password" to "\${db_password}", "username" to "\${username}", "password" to "\${password}")
    fun createExperimentForUnavailabilityStimulus(enrichedResilienceTestDefinition: EnrichedResilienceTestDefinition): CtkChaosExperiment {
        val secrets = createTopLevelSecrets()
        val steadyStateHypothesis = createSteadyStateHypothesisForUnaivalabilityStimulus(enrichedResilienceTestDefinition.enrichedProcessArtifact)
        val method = listOf(createActionToKillProcess(enrichedResilienceTestDefinition.enrichedProcessArtifact, enrichedResilienceTestDefinition.stimulus),
                createProbeToMonitorRecoveryTimeOfProcess(enrichedResilienceTestDefinition.enrichedProcessArtifact, enrichedResilienceTestDefinition.stimulus))
        val rollbacks = listOf(createActionToStartProcess(enrichedResilienceTestDefinition.enrichedProcessArtifact))
        val extensions = listOf(createExtensionHoldingResponesMeasureValues(enrichedResilienceTestDefinition.responseMeasure))
        val runtime = Runtime()
        // in this experiment we only want to rollback, if the steady-state-method deviates after method execution
        runtime.rollbacksStrategy = Strategy("deviated")

        return CtkChaosExperiment(enrichedResilienceTestDefinition.name, enrichedResilienceTestDefinition.description, secrets, steadyStateHypothesis, method, rollbacks, extensions, runtime)
    }

    /**
     * Creates an extension object which holds numerical values for expected response measures
     */
    private fun createExtensionHoldingResponesMeasureValues(responseMeasures: ResilienceResponseMeasures): ResponseMeasuresExtension? {

        val extension = ResponseMeasuresExtension()
        extension.setName("expected response measures")

        if (responseMeasures.recoveryTime != null){
            val recoveryTimeConstants = resilienceTestConstants.recoveryTime
            var expectedRecoveryTime: Int
            when (val responseTimeValue = responseMeasures.recoveryTime) {
                Satisfaction.SATISFIED -> expectedRecoveryTime = recoveryTimeConstants.satisfied
                Satisfaction.TOLERATED -> expectedRecoveryTime = recoveryTimeConstants.tolerated
                Satisfaction.FRUSTRATED-> expectedRecoveryTime = recoveryTimeConstants.frustrated
                else -> throw UnknownTermException(responseTimeValue.toString())
            }
            extension.setExpectedRecoveryTimeInMilliseconds(expectedRecoveryTime)
        }

        if (responseMeasures.responseTime != null){
            val responseTimeConstants = resilienceTestConstants.responseTime
            var expectedResponseTime: Int
            when (val responseTimeValue = responseMeasures.responseTime) {
                Satisfaction.SATISFIED -> expectedResponseTime = responseTimeConstants.satisfied
                Satisfaction.TOLERATED -> expectedResponseTime = responseTimeConstants.tolerated
                Satisfaction.FRUSTRATED-> expectedResponseTime = responseTimeConstants.frustrated
                else -> throw UnknownTermException(responseTimeValue.toString())
            }
            extension.setExpectedResponseTimeInMilliseconds(expectedResponseTime)
        }

        if (responseMeasures.errorRate != null){
            val errorRateConstants = resilienceTestConstants.errorRate
            var expectedErrorate: Int
            when (val errorRateValue = responseMeasures.errorRate) {
                Satisfaction.SATISFIED -> expectedErrorate = errorRateConstants.satisfied
                Satisfaction.TOLERATED -> expectedErrorate = errorRateConstants.tolerated
                Satisfaction.FRUSTRATED-> expectedErrorate = errorRateConstants.frustrated
                else -> throw UnknownTermException(errorRateValue.toString())
            }
            extension.setExpectedRecoveryTimeInMilliseconds(expectedErrorate)
        }

        return extension
    }

    /**
     * Creates a Secrets object which defines authentication secrets which are used by experiment probes and actions
     */
    private fun createTopLevelSecrets(): Secrets {
        // hardcoded secrets to enable authentication in python scripts against per default configured credentials in mySql
        val dbUsername = "aDBUser"
        val dbPassword = "aDBPw"
        val username = "demoUser"
        val password = "demo"
        val authenticationSecret = AuthenticationSecret(username, password, dbUsername, dbPassword)
        return Secrets(authenticationSecret)
    }

    private fun createActionToStartProcess(artifact: EnrichedProcessArtifact): Action {
        val actionName = "start process " + (artifact.processId)
        val argumentsForFunction =  authenticationParameters + ("path" to artifact.processPath) + ("log_result_in_influx_db" to true)
        val actionProvider = Provider("python", "processStarting", "start_process_by_path", argumentsForFunction)

        return Action(actionName, actionProvider)
    }


    fun createSteadyStateHypothesisForUnaivalabilityStimulus(artifact: EnrichedProcessArtifact): SteadyStateHypothesis {
        return SteadyStateHypothesis("Application is running", listOf(createProbeToLookIfProcessIsRunning(true, artifact)))
    }

    fun createProbeToLookIfProcessIsRunning(isSteadyStateHypothesis: Boolean, artifact: EnrichedProcessArtifact): Probe {
        val probeName = artifact.processId + " must be running"
        val argumentsForFunction =  authenticationParameters + ("process_name" to artifact.processId) + ("log_result_in_influx_db" to true)
        val probeProvider = Provider("python", "processMonitoring", "check_process_exists",argumentsForFunction)

        if (isSteadyStateHypothesis){
            val probeTolerance = ObjectMapper().convertValue<JsonNode>(true, JsonNode::class.java)
            return SteadyStateProbe(probeName, probeProvider, probeTolerance)
        }
        return Probe(probeName, probeProvider)
    }


    private fun createActionToKillProcess(artifact: EnrichedProcessArtifact, stimulus: ResilienceStimulus): Action {
        val actionName = "kill process " + artifact.processId
        val argumentsForFunction =  authenticationParameters + ("process_name" to artifact.processId)
        val actionProvider = Provider("python", "processKilling", "kill_process_by_name", argumentsForFunction)
        // for stimulus.experimentDurationSeconds see createProbeToMonitorRecoveryTimeOfProcess
        val pauses = Pauses(stimulus.pauseBeforeTriggeringSeconds,0)
        return Action(actionName, actionProvider, pauses)
    }

    private fun createProbeToMonitorRecoveryTimeOfProcess(artifact: EnrichedProcessArtifact, stimulus: ResilienceStimulus): Probe {
        val probeName = "measure duration until process " + artifact.processId + " is eventually available again"
        val argumentsForFunction =  authenticationParameters + ("process_name" to artifact.processId) +  ("monitoring_duration_sec" to stimulus.experimentDurationSeconds) +  ("checking_interval_sec" to 0)
        val provider = Provider("python", "processMonitoring", "get_duration_until_process_started", argumentsForFunction)
        return Probe(probeName, provider)
    }
}