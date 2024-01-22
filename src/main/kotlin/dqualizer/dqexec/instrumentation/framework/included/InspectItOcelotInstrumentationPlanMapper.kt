package dqualizer.dqexec.instrumentation.framework.included

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import dqualizer.dqexec.instrumentation.framework.InstrumentationMapper
import io.github.dqualizer.dqlang.types.instrumentation.Instrumentation
import okhttp3.internal.toImmutableMap
import org.mapstruct.Mapper
import org.springframework.stereotype.Component
import rocks.inspectit.ocelot.config.model.InspectitConfig
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings
import rocks.inspectit.ocelot.config.model.instrumentation.scope.ElementDescriptionMatcherSettings
import rocks.inspectit.ocelot.config.model.instrumentation.scope.InstrumentationScopeSettings
import rocks.inspectit.ocelot.config.model.instrumentation.scope.MatcherMode
import rocks.inspectit.ocelot.config.model.instrumentation.scope.MethodMatcherSettings

@Mapper
@Component
class InspectItOcelotInstrumentationPlanMapper : InstrumentationMapper<InspectItOcelotInstrumentationPlan> {
    override fun map(instrumentation: Instrumentation): InspectItOcelotInstrumentationPlan {

        val instrumentationSettings = InstrumentationSettings().apply {
            this.scopes = getScopes(instrumentation)
            this.actions = getActions(instrumentation)
            this.rules = getRules(instrumentation)
        }
        val config = InspectitConfig().apply { this.instrumentation = instrumentationSettings }
        return InspectItOcelotInstrumentationPlan(instrumentation, toYamlString(config));
    }


    private fun getScopes(instrumentation: Instrumentation): Map<String, InstrumentationScopeSettings> {
        val scopes = mutableMapOf<String, InstrumentationScopeSettings>()
        instrumentation.instrumentations.map { it.location }.forEach {
            val (className, methodSignature) = it.location.split("#", limit = 2)
            val (methodName, methodArguments) = methodSignature.split("(", limit = 2)
            //method arguments are ignored for now

            // create Superclass & Type Matcher
            val typeMatcher = ElementDescriptionMatcherSettings().apply {
                this.name = className
            }

            //create Method Matcher
            val methodMatcher = MethodMatcherSettings().apply {
                this.name = methodName
                this.matcherMode = MatcherMode.MATCHES
            }

            // combine them to named scope definition
            scopes["s_" + it.location] = InstrumentationScopeSettings().apply {
                this.methods = listOf(methodMatcher)
                this.superclass = typeMatcher
                this.type = typeMatcher
            }
        }
        return scopes.toImmutableMap()
    }

    //how to instrument (e.g. java code how to measure)
    private fun getActions(instrumentation: Instrumentation): Map<String, GenericActionSettings> {
        val actions = mutableMapOf<String, GenericActionSettings>()

        TODO("implement actions")

    }

    //where to execute the actions and what are the resulting metrics (including tags)
    private fun getRules(instrumentation: Instrumentation): MutableMap<String, InstrumentationRuleSettings> {
        val rules = mutableMapOf<String, InstrumentationRuleSettings>()

        TODO("implement rules")

    }


    private val yamlMapper =
        ObjectMapper(YAMLFactory()).apply { setSerializationInclusion(JsonInclude.Include.NON_NULL) }

    fun toYamlString(config: InspectitConfig): String {
        return yamlMapper.writeValueAsString(mapOf(Pair("inspectit", config)))
    }
}
