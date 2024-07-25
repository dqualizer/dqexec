package dqualizer.dqexec.resilience

data class CtkExperimentExecutorAPIResponse(
  val statusCode: Int,
  val status: String,
  val info: String
)