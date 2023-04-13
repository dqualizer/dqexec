package dqualizer.dqexec.exception

class UnknownRequestTypeException(type: String) : RuntimeException("Unknown request type: $type")
