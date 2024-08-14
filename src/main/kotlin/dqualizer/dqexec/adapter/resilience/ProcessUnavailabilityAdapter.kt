package dqualizer.dqexec.adapter.resilience

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.exception.UnknownTermException
import io.github.dqualizer.dqlang.types.adapter.constants.resilienceTest.ResilienceTestConstants
import io.github.dqualizer.dqlang.types.adapter.ctk.*
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.ProcessArtifact
import io.github.dqualizer.dqlang.types.rqa.configuration.resilience.ResilienceTestArtifact
import io.github.dqualizer.dqlang.types.rqa.definition.enums.Satisfaction
import io.github.dqualizer.dqlang.types.rqa.definition.resilience.ResilienceResponseMeasures
import io.github.dqualizer.dqlang.types.rqa.definition.resilience.stimulus.ResilienceStimulus
import org.springframework.stereotype.Component

@Component
class ProcessUnavailabilityAdapter(private val resilienceTestConstants: ResilienceTestConstants) {

  // $ pointers are used to reference the secretes defined in the top-level of the experiment definition
  val authenticationParameters = mapOf(
    "db_username" to "\${db_username}",
    "db_password" to "\${db_password}",
    "username" to "\${username}",
    "password" to "\${password}"
  )

  fun createExperimentForUnavailabilityStimulus(resilienceTestArtifact: ResilienceTestArtifact): CtkChaosExperiment {
    val secrets = createTopLevelSecrets()
    val processArtifact = resilienceTestArtifact.processArtifact!!
    val stimulus = resilienceTestArtifact.stimulus!!
    val steadyStateHypothesis = createSteadyStateHypothesisForUnaivalabilityStimulus(processArtifact)
    val method = listOf(createActionToKillProcess(processArtifact, stimulus),
            createProbeToMonitorRecoveryTimeOfProcess(processArtifact, stimulus))
    val rollbacks = listOf(createActionToStartProcess(processArtifact))
    val extensions = listOf(createExtensionHoldingResponesMeasureValues(resilienceTestArtifact.responseMeasure!!))
    val runtime = Runtime()

    // in this experiment we only want to rollback, if the steady-state-method deviates after method execution
    runtime.rollbacksStrategy = Strategy("deviated")

    return CtkChaosExperiment(resilienceTestArtifact.name, resilienceTestArtifact.description, secrets, steadyStateHypothesis, method, rollbacks, extensions, runtime)
  }

  /**
   * Creates an extension object which holds numerical values for expected response measures
   */
  private fun createExtensionHoldingResponesMeasureValues(responseMeasures: ResilienceResponseMeasures): ResponseMeasuresExtension {

    val extension = ResponseMeasuresExtension()
    extension.name = "expected response measures"

      if (responseMeasures.recoveryTime != null){
        val recoveryTimeConstants = resilienceTestConstants.recoveryTime!!
        var expectedRecoveryTime: Int
        when (val responseTimeValue = responseMeasures.recoveryTime) {
            Satisfaction.SATISFIED -> expectedRecoveryTime = recoveryTimeConstants.satisfied
            Satisfaction.TOLERATED -> expectedRecoveryTime = recoveryTimeConstants.tolerated
            Satisfaction.FRUSTRATED-> expectedRecoveryTime = recoveryTimeConstants.frustrated
            else -> throw UnknownTermException(responseTimeValue.toString())
        }
        extension.expectedRecoveryTimeInMilliseconds = expectedRecoveryTime
      }

      if (responseMeasures.responseTime != null){
        val responseTimeConstants = resilienceTestConstants.responseTime!!
        var expectedResponseTime: Int
        when (val responseTimeValue = responseMeasures.responseTime) {
            Satisfaction.SATISFIED -> expectedResponseTime = responseTimeConstants.satisfied
            Satisfaction.TOLERATED -> expectedResponseTime = responseTimeConstants.tolerated
            Satisfaction.FRUSTRATED-> expectedResponseTime = responseTimeConstants.frustrated
            else -> throw UnknownTermException(responseTimeValue.toString())
        }
        extension.expectedResponseTimeInMilliseconds = expectedResponseTime
      }

      if (responseMeasures.errorRate != null){
        val errorRateConstants = resilienceTestConstants.errorRate!!
        var expectedErrorRate: Int
        when (val errorRateValue = responseMeasures.errorRate) {
            Satisfaction.SATISFIED -> expectedErrorRate = errorRateConstants.satisfied
            Satisfaction.TOLERATED -> expectedErrorRate = errorRateConstants.tolerated
            Satisfaction.FRUSTRATED-> expectedErrorRate = errorRateConstants.frustrated
            else -> throw UnknownTermException(errorRateValue.toString())
        }
        extension.expectedErrorRateInPercent = expectedErrorRate
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

  private fun createActionToStartProcess(artifact: ProcessArtifact): Action {
      val actionName = "start process " + (artifact.processName)
      val argumentsForFunction =  authenticationParameters + ("path" to artifact.processPath)
      val actionProvider = Provider("python", "processStarting", "start_jvm_process_by_path", argumentsForFunction)
      // Hardcoded pause to restart process, since it can't be null
      val pauses = Pauses(2, 5)

      return Action(name = actionName, provider = actionProvider, pauses = pauses)
  }


  fun createSteadyStateHypothesisForUnaivalabilityStimulus(artifact: ProcessArtifact): SteadyStateHypothesis {
      return SteadyStateHypothesis("Application is running", listOf(createProbeToLookIfProcessIsRunning(true, artifact)))
  }

  fun createProbeToLookIfProcessIsRunning(isSteadyStateHypothesis: Boolean, artifact: ProcessArtifact): Probe {
      val probeName = artifact.processName + " must be running"
      val argumentsForFunction =  authenticationParameters + ("process_name" to artifact.processName) + ("log_result_in_influx_db" to true)
      val probeProvider = Provider("python", "processMonitoring", "check_process_exists",argumentsForFunction)

      if (isSteadyStateHypothesis){
          val probeTolerance = ObjectMapper().convertValue<JsonNode>(true, JsonNode::class.java)
          return SteadyStateProbe(probeName, probeProvider, probeTolerance)
      }
      return Probe(name = probeName, provider =  probeProvider)
  }


  private fun createActionToKillProcess(artifact: ProcessArtifact, stimulus: ResilienceStimulus): Action {
      val actionName = "kill process " + artifact.processName
      val argumentsForFunction =  authenticationParameters + ("process_name" to artifact.processName)
      val actionProvider = Provider("python", "processKilling", "kill_process_by_name", argumentsForFunction)
      // for stimulus.experimentDurationSeconds see createProbeToMonitorRecoveryTimeOfProcess
      val pauses = Pauses(stimulus.pauseBeforeTriggeringSeconds,0)
      return Action(name = actionName, pauses =  pauses, provider = actionProvider)
  }

  private fun createProbeToMonitorRecoveryTimeOfProcess(artifact: ProcessArtifact, stimulus: ResilienceStimulus): Probe {
      val probeName = "measure duration until process " + artifact.processName + " is eventually available again"
      val argumentsForFunction =  authenticationParameters + ("process_name" to artifact.processName) +  ("monitoring_duration_sec" to stimulus.experimentDurationSeconds) +  ("checking_interval_sec" to 0)
      val provider = Provider("python", "processMonitoring", "get_duration_until_process_started", argumentsForFunction)
      return Probe(name = probeName, provider = provider)
  }
}