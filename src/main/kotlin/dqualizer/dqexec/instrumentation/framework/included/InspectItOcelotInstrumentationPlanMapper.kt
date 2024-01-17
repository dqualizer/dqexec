package dqualizer.dqexec.instrumentation.framework.included

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.ServiceMonitoringConfiguration
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.instrumentation.InstrumentType
import io.github.dqualizer.dqlang.types.rqa.definition.monitoring.MeasurementType
import okhttp3.internal.toImmutableMap
import org.mapstruct.Mapper
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import rocks.inspectit.ocelot.config.model.InspectitConfig
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings
import rocks.inspectit.ocelot.config.model.instrumentation.scope.ElementDescriptionMatcherSettings
import rocks.inspectit.ocelot.config.model.instrumentation.scope.InstrumentationScopeSettings
import rocks.inspectit.ocelot.config.model.instrumentation.scope.MatcherMode
import rocks.inspectit.ocelot.config.model.instrumentation.scope.MethodMatcherSettings
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings.ViewDefinitionSettingsBuilder


@Mapper
@Component
class InspectItOcelotInstrumentationPlanMapper {


    fun map(instrumentation: ServiceMonitoringConfiguration, contextID: String): InspectItOcelotInstrumentationPlan {

        val resourceLoader: ResourceLoader = DefaultResourceLoader()
        val resource: Resource = resourceLoader.getResource("classpath:exampleconfig.yaml")
        val content = String(resource.contentAsByteArray)


        val yamlMapper = ObjectMapper(YAMLFactory())
        yamlMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)

        var configMap = yamlMapper.readValue<Map<String, MutableMap<String,Any>>>(content).toMutableMap()

        configMap["inspectit"]?.remove("instrumentation")
//        configMap["inspectit"]?.remove("metrics")
//        configMap["inspectit"]?.remove("tracing")
//        configMap["inspectit"]?.remove("logging")



        val jsonMapper = ObjectMapper()
        //ignore null values
        jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
        val json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(configMap)

        return InspectItOcelotInstrumentationPlan(instrumentation, json)


//        val metrics = generateMetrics(instrumentation)
//
//        val instrumentationSettings = InstrumentationSettings().apply {
//            this.scopes = getScopes(instrumentation)
//            this.actions = getActions(instrumentation, contextID)
//            this.rules = getRules(instrumentation, metrics, scopes, actions)
//        }
//        val config = InspectitConfig().apply {
//            this.instrumentation = instrumentationSettings
//            this.metrics = metrics
//        }
//        return InspectItOcelotInstrumentationPlan(instrumentation, toYamlString(config));
    }

    private fun generateMetrics(instrumentation: ServiceMonitoringConfiguration): MetricsSettings {
        val definitions = instrumentation.instruments.associate {
            val type = it.measurementType
            val instrumentType = it.instrumentType


            val definitionNameTemplate = getMetricNameTemplateFromType(type)

            val viewBuilders = mutableMapOf<String, ViewDefinitionSettingsBuilder>()
            when (instrumentType) {
                InstrumentType.GAUGE -> {
                    viewBuilders[definitionNameTemplate.format("/last_value")] =
                        ViewDefinitionSettings.builder()
                            .aggregation(ViewDefinitionSettings.Aggregation.LAST_VALUE)
                    viewBuilders[definitionNameTemplate.format("/sum")] =
                        ViewDefinitionSettings.builder()
                            .aggregation(ViewDefinitionSettings.Aggregation.SUM)
                }

                InstrumentType.COUNTER -> {
                    viewBuilders[definitionNameTemplate.format("/count")] =
                        ViewDefinitionSettings.builder()
                            .aggregation(ViewDefinitionSettings.Aggregation.COUNT)
                    viewBuilders[definitionNameTemplate.format("/sum")] =
                        ViewDefinitionSettings.builder()
                            .aggregation(ViewDefinitionSettings.Aggregation.SUM)
                }

                InstrumentType.HISTOGRAM -> {
                    viewBuilders[definitionNameTemplate.format("/hist")] =
                        ViewDefinitionSettings.builder()
                            .aggregation(ViewDefinitionSettings.Aggregation.HISTOGRAM)
                            .bucketBoundaries(it.histogramBuckets)
                }
            }

            //Create common default tags
            val defaultTagConfiguration = mapOf(
                Pair("context", true),
                Pair("component", true),
                Pair("measurement_name", true)
            )
            val views = viewBuilders.map { entry ->
                val builder = entry.value
                builder.tags(defaultTagConfiguration)
                Pair(entry.key, builder.build())
            }.toMap()

            val metricDefinitionSettings = MetricDefinitionSettings.builder()
                .type(MetricDefinitionSettings.MeasureType.DOUBLE)
                .unit(it.measurementUnit)
                .views(views)
                .build()

            Pair(definitionNameTemplate.format(""), metricDefinitionSettings);
        }


        return MetricsSettings().apply {
            this.isEnabled = true
            this.definitions = definitions
            //TODO: add option for resource monitoring
        }
    }

    private fun getMetricNameTemplateFromType(type: MeasurementType) = when (type) {
        MeasurementType.VALUE_INSPECTION -> {
            "[value_inspection%s]"
        }

        MeasurementType.EXECUTION_COUNT -> {
            "[execution_count%s]"
        }

        MeasurementType.EXECUTION_TIME -> {
            "[execution_time%s]"
        }
    }

    private fun getMetricFromType(settings: MetricsSettings, type: MeasurementType): MetricDefinitionSettings {
        val name = getMetricNameTemplateFromType(type).format("")

        return settings.definitions.computeIfAbsent(name) {
            throw IllegalArgumentException("Metric with name $name not found")
        }
    }


    private fun getScopes(instrumentation: ServiceMonitoringConfiguration): Map<String, InstrumentationScopeSettings> {
        val scopes = mutableMapOf<String, InstrumentationScopeSettings>()
        instrumentation.instruments.map { it.location }.forEach {
            val location = Location.fromString(it.location)

            // create Superclass & Type Matcher
            val typeMatcher = ElementDescriptionMatcherSettings().apply {
                this.name = location.classIdentifier
                this.matcherMode = MatcherMode.ENDS_WITH_IGNORE_CASE
            }

            //create Method Matcher
            val methodMatcher = MethodMatcherSettings().apply {
                this.name = location.methodName
                this.matcherMode = MatcherMode.EQUALS_FULLY_IGNORE_CASE
            }

            // combine them to named scope definition
            scopes["s_" + it.location] = InstrumentationScopeSettings().apply {
                this.methods = listOf(methodMatcher)
                this.type = typeMatcher
            }
        }
        return scopes.toImmutableMap()
    }

    data class Location(val classIdentifier: String, val methodName: String, val methodArguments: String) {
        companion object {
            fun fromString(location: String): Location {
                val (classIdentifier, methodSignature) = location.split("#", limit = 2)
                val (methodName, methodArguments) = methodSignature.split("(", limit = 2)
                return Location(classIdentifier, methodName, methodArguments)
            }
        }
    }

    private fun sanitizeToString(any: Any): String {
        return sanitizeName(any.toString())
    }

    private fun sanitizeName(name: String): String {
        return name.lowercase().replace("[^a-z0-9]".toRegex(), "_")
    }

    //how to instrument (e.g. java code how to measure)
    private fun getActions(
        instrumentation: ServiceMonitoringConfiguration,
        contextID: String
    ): Map<String, GenericActionSettings> {
        val actions = mutableMapOf<String, GenericActionSettings>()

        actions["a_get_context_name"] = GenericActionSettings().apply {
            this.value = contextID
        }
        actions["a_timestamp_ms"] = GenericActionSettings().apply {
            this.value = "Long.valueOf(System.currentTimeMillis())"
        }

        instrumentation.instruments.forEach {
            it.measurementName
            actions["a_get_measurement_name_" + sanitizeName(it.measurementName)] = GenericActionSettings().apply {
                this.value = it.measurementName
            }
        }

        return actions
    }

    //where to execute the actions and what are the resulting metrics (including tags)
//    private fun getRules(
//        instrumentation: ServiceMonitoringConfiguration,
//        metrics: MetricsSettings,
//        scopes: Map<String, InstrumentationScopeSettings>,
//        actions: Map<String, GenericActionSettings>
//    ): Map<String, InstrumentationRuleSettings> {
//        val rules = mutableMapOf<String, InstrumentationRuleSettings>()
//
//        instrumentation.instruments.map {
//            val measurementTypeName = sanitizeToString(it.measurementType)
//            val location = Location.fromString(it.location.location)
//            val instrumentLocationName = sanitizeToString(location)
//            val ruleName = "r_" + measurementTypeName + "_" + instrumentLocationName
//
//            val resultValueName = "result_value"
//
//            val scope = "s_" + location.methodName
//            val metric = getMetricFromType(metrics, it.measurementType)
//
//            val metricRecordingSettings = MetricRecordingSettings().apply {
//                this.metric = getMetricNameTemplateFromType(it.measurementType).format("")
//                this.value = resultValueName
//                this.dataTags = mapOf(
//                    Pair("class", "value")
//                )
//                this.constantTags = mapOf(
//                    Pair("context", "\${a_get_context_name}"),
//                    Pair("component", it.targetComponentId),
//                    Pair("measurement_name", it.measurementName))
//            }
//
//            val (entry,exit) = when (it.measurementType) {
//                MeasurementType.EXECUTION_TIME -> {
//                    Pair(ActionCallSettings().apply { this.action = "a_timestamp_ms"},
//                        ActionCallSettings().apply { this.action = "a_timestamp_ms"})
//                }
//                else -> {
//                    Pair(null, null)
//                }
//            }
//
//
//            return@map InstrumentationRuleSettings().apply {
//                this.scopes = mutableMapOf(Pair(scope, true))
//                this.metrics =
//                    this.ent
//            }
//        }
//
//        return rules.toImmutableMap()
//    }

    private fun getRules(
        instrumentation: ServiceMonitoringConfiguration,
        metrics: MetricsSettings,
        scopes: Map<String, InstrumentationScopeSettings>,
        actions: Map<String, GenericActionSettings>
    ): Map<String, InstrumentationRuleSettings> {
        return mapOf()
    }


    private val yamlMapper =
        ObjectMapper(YAMLFactory()).apply { setSerializationInclusion(JsonInclude.Include.NON_NULL) }

    fun toYamlString(config: InspectitConfig): String {
        return yamlMapper.writeValueAsString(mapOf(Pair("inspectit", config)))
    }
}
