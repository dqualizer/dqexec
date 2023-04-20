package dqualizer.dqexec.loadtest.mapper.k6

import dqualizer.dqexec.RuntimeQualityAnalysisConfiguration

/**
 * An interface for all mappers necessary for creating a k6-script
 */
@FunctionalInterface
interface RuntimeQualityAnalysisConfigurationTranslator {
    /**
     * Map one part of request object to a String, which can be written inside a Javascript file
     * @param request
     * @return String that can be written inside a Javascript file
     */
    fun map(request: RuntimeQualityAnalysisConfiguration): String
}