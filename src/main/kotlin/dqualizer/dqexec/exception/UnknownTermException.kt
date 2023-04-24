package dqualizer.dqexec.exception

class UnknownTermException(term: String) : RuntimeException("Unknown term: $term")