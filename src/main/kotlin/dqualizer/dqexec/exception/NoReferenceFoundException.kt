package dqualizer.dqexec.exception

class NoReferenceFoundException(referenceMap: Map<String, String>) :
  RuntimeException("No referenc could be found: $referenceMap")
