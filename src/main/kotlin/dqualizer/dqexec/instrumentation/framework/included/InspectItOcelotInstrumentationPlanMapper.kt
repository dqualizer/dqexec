package dqualizer.dqexec.instrumentation.framework.included

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import dqualizer.dqexec.util.take
import io.github.dqualizer.dqlang.types.dam.DomainArchitectureMapping
import io.github.dqualizer.dqlang.types.dam.architecture.CodeComponent
import io.github.dqualizer.dqlang.types.dam.mapping.ActivityToCallMapping
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.ServiceMonitoringConfiguration
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.instrumentation.Instrument
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.instrumentation.InstrumentLocation
import io.github.dqualizer.dqlang.types.rqa.configuration.monitoring.instrumentation.InstrumentType
import io.github.dqualizer.dqlang.types.rqa.definition.monitoring.MeasurementType
import io.opentelemetry.api.trace.SpanKind
import okhttp3.internal.toImmutableMap
import org.mapstruct.Mapper
import org.springframework.stereotype.Component
import rocks.inspectit.ocelot.config.model.InspectitConfig
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings
import rocks.inspectit.ocelot.config.model.instrumentation.rules.InstrumentationRuleSettings
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings
import rocks.inspectit.ocelot.config.model.instrumentation.scope.ElementDescriptionMatcherSettings
import rocks.inspectit.ocelot.config.model.instrumentation.scope.InstrumentationScopeSettings
import rocks.inspectit.ocelot.config.model.instrumentation.scope.MatcherMode
import rocks.inspectit.ocelot.config.model.instrumentation.scope.MethodMatcherSettings
import rocks.inspectit.ocelot.config.model.logging.LoggingSettings
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings.ViewDefinitionSettingsBuilder
import rocks.inspectit.ocelot.config.model.tracing.LogCorrelationSettings
import rocks.inspectit.ocelot.config.model.tracing.PropagationFormat
import rocks.inspectit.ocelot.config.model.tracing.TraceIdAutoInjectionSettings
import rocks.inspectit.ocelot.config.model.tracing.TracingSettings
import java.util.*


@Mapper
@Component
class InspectItOcelotInstrumentationPlanMapper {

  private val yamlMapper =
    ObjectMapper().apply {
      // setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
    }

  fun toYamlString(config: InspectitConfig): String {
    return yamlMapper.writeValueAsString(mapOf(Pair("inspectit", config)))
  }


  private fun InstrumentLocation.toScopeName() = "s_" + sanitizeName(this.location)

  private fun Instrument.toActionName() = "a_get_" + sanitizeName(this.measurementName)

  private fun Instrument.getMeasurementNameActionName() =
    "a_get_measurement_name_" + sanitizeName(this.measurementName)

  private fun sanitizeName(name: String): String {
    return name.lowercase().replace(Regex("[^a-z0-9]"), "_")
  }

  fun map(
    instrumentation: ServiceMonitoringConfiguration,
    dam: DomainArchitectureMapping
  ): InspectItOcelotInstrumentationPlan {

    val metrics = generateMetrics(instrumentation)

    val instrumentationSettings = InstrumentationSettings().apply {
      this.scopes = generateScopes(dam, instrumentation)
      this.actions = generateActions(dam, instrumentation)
      this.rules = getRules(dam, instrumentation, metrics, scopes, actions)

    }
    val config = InspectitConfig().apply {
      this.instrumentation = instrumentationSettings
      this.metrics = metrics
      this.logging = LoggingSettings().apply { this.isDebug = true }
      this.tracing = TracingSettings().createTraceSettings(instrumentation)
    }

    val yamlString = toYamlString(config)
    return InspectItOcelotInstrumentationPlan(instrumentation, yamlString);
  }

  private fun TracingSettings.createTraceSettings(instrumentation: ServiceMonitoringConfiguration): TracingSettings {
    isEnabled = instrumentation.instruments.any { it.measurementType == MeasurementType.EXECUTION_TIME }
      || instrumentation.instrumentationFramework.hasTraces

    propagationFormat = PropagationFormat.B3

    logCorrelation = LogCorrelationSettings().apply {
      traceIdAutoInjection = TraceIdAutoInjectionSettings().apply {
        this.isEnabled = true
        this.prefix = "[TraceID:"
        this.suffix = "]"
      }
    }
    return this
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


  private fun generateScopes(
    dam: DomainArchitectureMapping,
    instrumentation: ServiceMonitoringConfiguration
  ): Map<String, InstrumentationScopeSettings> {
    val scopes = mutableMapOf<String, InstrumentationScopeSettings>()
    instrumentation.instruments.forEach { instrument ->
      val loc = instrument.location
      val location = JavaLocation.fromString(loc.location)

      // create Superclass & Type Matcher
      val typeMatcher = ElementDescriptionMatcherSettings().apply {
        this.name = location.classIdentifier
        this.matcherMode = MatcherMode.ENDS_WITH_IGNORE_CASE
      }

      if (location.methodName.isEmpty) {
        throw IllegalArgumentException("Method name is empty in location: $loc (regarding instrument ${instrument.instrumentName})")
      }

      //create Method Matcher
      val methodMatcher = MethodMatcherSettings().apply {
        this.name = location.methodName.get()
        this.matcherMode = MatcherMode.EQUALS_FULLY_IGNORE_CASE
      }

      // combine them to named scope definition
      scopes[loc.toScopeName()] = InstrumentationScopeSettings().apply {
        this.methods = listOf(methodMatcher)
        this.type = typeMatcher
      }

      //if the target gets called asynchronously, we need to add another fitting scope to the target
      val mappingCause = dam.mapper.getMappings(instrument.targetComponentId)
      Optional.ofNullable(mappingCause
        .filterIsInstance<ActivityToCallMapping>()
        .firstOrNull { it.isAsync() })
        .ifPresent {
          val end = dam.softwareSystem.findArchitectureEntityOfType<CodeComponent>(it.end!!).get()
          val endScopeName = "s_" + sanitizeName(end.name) + "_end"

          val endLocation = JavaLocation.fromString(end.identifier)

          val endTypeMatcher = ElementDescriptionMatcherSettings().apply {
            this.name = endLocation.classIdentifier
            this.matcherMode = MatcherMode.ENDS_WITH_IGNORE_CASE
          }

          val endMethodMatcher = MethodMatcherSettings().apply {
            this.name = endLocation.methodName.get()
            this.matcherMode = MatcherMode.EQUALS_FULLY_IGNORE_CASE
          }

          scopes[endScopeName] = InstrumentationScopeSettings().apply {
            this.methods = listOf(endMethodMatcher)
            this.type = endTypeMatcher
          }
        }
    }
    return scopes.toImmutableMap()
  }


  //how to instrument (e.g. java code how to measure)
  private fun generateActions(
    dam: DomainArchitectureMapping,
    instrumentation: ServiceMonitoringConfiguration
  ): Map<String, GenericActionSettings> {
    val actions = mutableMapOf<String, GenericActionSettings>()

    //default actions
    actions["a_get_context_name"] = GenericActionSettings().apply {
      this.value = dam.id
    }
    actions["a_timestamp_ms"] = GenericActionSettings().apply {
      this.value = "Long.valueOf(System.currentTimeMillis())"
    }
    actions["a_calculate_time_difference"] = GenericActionSettings().apply {
      this.value = "Long.valueOf(System.currentTimeMillis() - timestamp)"
      this.input = mapOf(Pair("timestamp", "long"))
    }

    //component name actions
    dam.softwareSystem.architectureEntities.forEach { entity ->
      val entityName = if (entity is CodeComponent) take { entity.name } else entity.id
      actions["a_get_entity_name_" + entity.id] = GenericActionSettings().apply {
        this.value = entityName
      }
    }

    //measurement name actions
    instrumentation.instruments
      .forEach { instrument ->
        actions[instrument.getMeasurementNameActionName()] =
          GenericActionSettings().apply {
            this.value = instrument.measurementName
          }
      }

    //generate correlation id actions
    instrumentation.instruments
      .filter { it.measurementType == MeasurementType.EXECUTION_TIME }
      .forEach { instrument ->
        //generate correlation id retrieving actions
        val mappingCause = dam.mapper.getMappings(instrument.targetComponentId)
        Optional.ofNullable(mappingCause
          .filterIsInstance<ActivityToCallMapping>()
          .firstOrNull { it.isAsync() })
          .ifPresent {
            //start correlation id action
            val startCorrelationIdActionName =
              "a_get_correlation_id_" + sanitizeName(instrument.measurementName) + "_start"

            actions[startCorrelationIdActionName] = GenericActionSettings().apply {
              this.value = it.startCorrelationId
            }

            //end exit action
            val endCorrelationIdActionName =
              "a_get_correlation_id_" + sanitizeName(instrument.measurementName) + "_end"
            actions[endCorrelationIdActionName] = GenericActionSettings().apply {
              this.value = it.endCorrelationId
            }
          }
      }


    return actions.toImmutableMap()
  }


  private fun getRules(
    dam: DomainArchitectureMapping,
    instrumentation: ServiceMonitoringConfiguration,
    metrics: MetricsSettings,
    scopes: Map<String, InstrumentationScopeSettings>,
    actions: Map<String, GenericActionSettings>
  ): Map<String, InstrumentationRuleSettings> {
    val rules = mutableMapOf<String, InstrumentationRuleSettings>()

    instrumentation.instruments.forEach { instrument ->

      when (instrument.measurementType) {
        MeasurementType.VALUE_INSPECTION -> {
          val measurementName = sanitizeName(instrument.measurementName)
          val ruleName = "r_$measurementName"

          rules[ruleName] = handleValueInspection(dam, instrument)
        }

        MeasurementType.EXECUTION_TIME -> {
          handleExecutionTimeInstrumentation(dam, instrument)
            .forEach { (k, v) -> rules[k] = v }
        }

        MeasurementType.EXECUTION_COUNT -> {
          TODO()
        }
      }

    }

    return rules.toImmutableMap()
  }

  private fun handleExecutionTimeInstrumentation(
    dam: DomainArchitectureMapping,
    instrument: Instrument
  ): Map<String, InstrumentationRuleSettings> {

    val mappingCause = dam.mapper.getMappings(instrument.targetComponentId)

    //this is only used for casting as the mapping cause should always be an activity to call mapping
    val activityToCallMappings = mappingCause.filterIsInstance<ActivityToCallMapping>()
    if (activityToCallMappings.any { it.isAsync() }) {
      //asynchronous: measure start of start and end of end Method -> 2 rules
      val mapping = activityToCallMappings.first { it.isAsync() }

      val start = dam.softwareSystem.findArchitectureEntityOfType<CodeComponent>(mapping.architectureElementId!!)
      val end = dam.softwareSystem.findArchitectureEntityOfType<CodeComponent>(mapping.end!!)

      val random_uuid = UUID.randomUUID().toString()

      //time entry rule
      val entryRule = createEntryTracingRule(start, mapping, instrument, dam)
      entryRule.tracing.storeSpan = random_uuid

      //time exit rule
      val exitRule = createExitTracingRule(end, mapping, instrument, dam)
      exitRule.tracing.continueSpan = random_uuid

      return mapOf(
        Pair("r_" + sanitizeName(instrument.measurementName) + "_entry", entryRule),
        Pair("r_" + sanitizeName(instrument.measurementName) + "_exit", exitRule)
      )

    }

    //synchronous: measure start and end of Method -> 1 rule
    val ruleName = "r_" + sanitizeName(instrument.measurementName)

    val instrumentationRuleSettings = InstrumentationRuleSettings().apply {
      this.entry = mapOf(getSimpleAction("method_entry_time", "a_timestamp_ms"))
      this.exit = mapOf(Pair("duration", ActionCallSettings().apply {
        this.action = "a_calculate_time_difference"
        this.dataInput = mapOf(Pair("timestamp", "method_entry_time"))
      }))
      this.scopes = mapOf(Pair(instrument.location.toScopeName(), true))
      this.metrics = mapOf(
        Pair(
          "[execution_time]",
          MetricRecordingSettings().apply {
            this.value = "duration"
            this.constantTags = mapOf(
              Pair("context", dam.id),
              Pair("component_id", instrument.targetComponentId),
              Pair("measurement_name", instrument.measurementName)
            )
          }
        )
      )
    }

    return mapOf(Pair(ruleName, instrumentationRuleSettings))
  }


  private fun createEntryTracingRule(
    start: Optional<CodeComponent>,
    mapping: ActivityToCallMapping,
    instrument: Instrument,
    dam: DomainArchitectureMapping
  ): InstrumentationRuleSettings {

    val measurementName = instrument.measurementName
    val entryActions = mapOf(
      getSimpleAction("method_entry_time", "a_timestamp_ms"),
      getSimpleAction("correlation_id", "a_get_correlation_id_" + sanitizeName(measurementName) + "_start"),
      getSimpleAction("measurement_name", "a_get_measurement_name_" + sanitizeName(measurementName)),
      getSimpleAction("context_name", "a_get_context_name"),
      getSimpleAction("component_name", "a_get_enity_name_" + instrument.targetComponentId)
    )

    val scopeName = instrument.location.toScopeName()
    val scopes = mapOf(Pair(scopeName, true))

    val tracingSettings = RuleTracingSettings().apply {
      this.name = "correlation_id"
      this.startSpan = true
      this.endSpan = false
      this.kind = SpanKind.SERVER
      this.attributes = mapOf(
        Pair("correlation_id", "correlation_id"),
        Pair("start_time_ms", "method_entry_time"),
        Pair("context", "context_name"),
        Pair("component", "component_name"),
        Pair("measurement_name", "measurement_name")
      )
    }


    return InstrumentationRuleSettings().apply {
      this.scopes = scopes
      this.tracing = tracingSettings
      this.entry = entryActions
      this.include = mapOf(Pair("r_tracing_global_attributes", true))
    }
  }

  private fun createExitTracingRule(
    end: Optional<CodeComponent>,
    mapping: ActivityToCallMapping,
    instrument: Instrument,
    dam: DomainArchitectureMapping
  ): InstrumentationRuleSettings {
    val measurementName = instrument.measurementName
    val exitActions = mapOf(
      getSimpleAction("method_exit_time", "a_timestamp_ms"),
      getSimpleAction("correlation_id", "a_get_correlation_id_" + sanitizeName(measurementName) + "_end"),
      getSimpleAction("measurement_name", "a_get_measurement_name_" + sanitizeName(measurementName)),
      getSimpleAction("context_name", "a_get_context_name"),
      getSimpleAction("component_name", "a_get_enity_name_" + instrument.targetComponentId)
    )

    val scopeName = instrument.location.toScopeName()
    val scopes = mapOf(Pair(scopeName, true))

    val tracingSettings = RuleTracingSettings().apply {
      this.name = "correlation_id"
      this.endSpan = true
      this.kind = SpanKind.SERVER
      this.attributes = mapOf(
        Pair("correlation_id", "correlation_id"),
        Pair("end_time_ms", "method_entry_time"),
        Pair("context", "context_name"),
        Pair("component", "component_name"),
        Pair("measurement_name", "measurement_name")
      )
    }


    return InstrumentationRuleSettings().apply {
      this.scopes = scopes
      this.tracing = tracingSettings
      this.entry = exitActions
      this.include = mapOf(Pair("r_tracing_global_attributes", true))
    }

  }

  private fun handleValueInspection(
    dam: DomainArchitectureMapping,
    instrument: Instrument
  ): InstrumentationRuleSettings {

    val scopeName = instrument.location.toScopeName()

    val exitActions = mapOf(
      getSimpleAction("result_value", instrument.toActionName())
    )

    val scopes = mapOf(Pair(scopeName, true))

    val metricsSettings = mapOf(
      Pair(
        "[value_inspection]",
        MetricRecordingSettings().apply {
          this.value = "result_value"
          this.dataTags = mapOf(
            Pair("class", "value")
          )
          val targetComponent =
            dam.softwareSystem.findArchitectureEntity(instrument.targetComponentId)

          this.constantTags = mutableMapOf(
            Pair("context", dam.id),
            Pair("component_id", instrument.targetComponentId),
            Pair("measurement_name", instrument.measurementName)
          )
          targetComponent
            .filter { it is CodeComponent }
            .map(CodeComponent::class.java::cast)
            .ifPresent { this.constantTags["component"] = it.name }

        })
    )

    return InstrumentationRuleSettings().apply {
      this.scopes = scopes
      this.metrics = metricsSettings
      this.exit = exitActions
    }
  }

  private fun getSimpleAction(
    first: String,
    second: String
  ) = Pair(
    first,
    ActionCallSettings().apply {
      this.action = second
    })
}
