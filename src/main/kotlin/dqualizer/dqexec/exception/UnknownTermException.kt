package dqualizer.dqexec.exception

import java.lang.RuntimeException

class UnknownTermException(term: String) : RuntimeException("Unknown term: $term")