package dqualizer.dqexec.instrumentation.framework.included

import org.apache.commons.lang3.StringUtils
import java.util.*

data class JavaLocation(
    val classIdentifier: String,
    val methodName: Optional<String>,
    val methodParameters: Optional<String>
) {
    companion object {
        fun fromString(location: String): JavaLocation {
            if (!location.contains("#")) {
                return JavaLocation(location, Optional.empty(), Optional.empty())
            }

            val (classIdentifier, methodSignature) = location.split("#", limit = 2)

            if (StringUtils.isAllBlank(classIdentifier))
                throw IllegalArgumentException("Class identifier is empty in location: $location")

            if (StringUtils.isAllBlank(methodSignature))
                throw IllegalArgumentException("Method signature is empty in location: $location")

            val (methodName, methodArguments) =
                if (methodSignature.contains("(")) {
                    methodSignature.split("(", limit = 2)
                        .let { Pair(it[0], Optional.of("(" + it[1])) }
                } else {
                    Pair(methodSignature, Optional.empty())
                }

            return JavaLocation(classIdentifier, Optional.of(methodName), methodArguments)
        }
    }
}
